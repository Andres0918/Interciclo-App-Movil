package com.compuinside.auth.jwt;

import com.compuinside.auth.controller.*;
import com.compuinside.auth.dto.Severity;
import com.compuinside.auth.dto.UserEvent;
import com.compuinside.auth.exception.CompuInsideCustomException;
import com.compuinside.auth.repository.Token;
import com.compuinside.auth.repository.TokenRepository;
import com.compuinside.auth.services.FeignClientService;
import com.compuinside.auth.user.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsersRepository usersRepository;
    private final AccountRepository accountsRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    //private final KafkaProducerService kafkaProducerService;
    private final FeignClientService feignClientService;
    private final EmpresaRepository empresaRepository;
    private final TokenRepository tokenRepository;

    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Usuario intenta loguearse: " + loginRequest.getUsername());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            throw new CompuInsideCustomException(
                    "Credenciales inválidas",
                    "El usuario o la contraseña son incorrectos",
                    Severity.ERROR
            );
        } catch (AuthenticationException e) {
            throw new CompuInsideCustomException(
                    "Error de autenticación",
                    "Ocurrió un problema al autenticar al usuario",
                    Severity.ERROR
            );
        }

        // Buscamos el User, no el Account
        User user = usersRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new CompuInsideCustomException(
                        "Usuario no existe",
                        "No se encontró un usuario con los datos proporcionados",
                        Severity.WARNING
                ));

        // Generamos token desde el User
        String token = jwtService.getToken(user);
        String refreshToken = jwtService.getRefreshToken(user);
        revokeAllUserTokens(user);
        revokeAllUserTokens(user);
        saveUserToken(user, token, false);
        saveUserToken(user, refreshToken, true);
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse createUserWithExistingAccount(AccountRequest dto) {
        // Buscar account existente
        log.info("Cuenta perteneciente a " + dto.getEmpresa() + "intenta crear nuevo usuario ");
        UUID accountId = UUID.fromString(dto.getAccountId());
        Account account = accountsRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account no encontrado con id " + accountId));

        // Crear usuario asociado al account existente
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .state(State.ACTIVE)
                .email(dto.getEmail())
                .serviceClient(dto.getServiceClient())
                .account(account) // 👈 usamos el account existente
                .build();

        user = usersRepository.saveAndFlush(user);

        UserEvent event = UserEvent.builder()
                .userId(user.getId())
                .role(user.getRole().toString())
                .serviceClient(user.getServiceClient().toString())
                .userPlan(account.getUserPlan().toString())
                .action("CREATE")
                .build();

        String token = jwtService.getToken(user);
        String refreshToken = jwtService.getRefreshToken(user);
        saveUserToken(user, token, false);
        saveUserToken(user, refreshToken, true);
        feignClientService.syncUser(event);
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse createUserWithNewAccount(RegisterRequest dto) {
        log.info("Se inicia registro de nueva cuenta con usuario master ");
        //UUID empresaId = UUID.fromString(dto.getEmpresa());
        //Empresa empresa = empresaRepository.findById(empresaId)
        //        .orElseThrow(() -> new CompuInsideCustomException(
         //               "Empresa no encontrada",
           //             "La empresa no existe",
             //           Severity.ERROR
               // ));

        // Crear nuevo account ligado a la empresa
        Account account = Account.builder()
                //.empresa(empresa)
                .userPlan(dto.getUserPlan()) // o puede venir del dto
                .state(State.ACTIVE)
                .build();

        account = accountsRepository.saveAndFlush(account);

        // Crear usuario ligado al nuevo account
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .state(State.ACTIVE)
                .email(dto.getEmail())
                .serviceClient(dto.getServiceClient())
                .account(account)
                .build();

        user = usersRepository.saveAndFlush(user);
        String token = jwtService.getToken(user);
        String refreshToken = jwtService.getRefreshToken(user);
        saveUserToken(user, token, false);
        saveUserToken(user, refreshToken, true);
        UserEvent event = UserEvent.builder()
                .userId(user.getId())
                .role(user.getRole().toString())
                .serviceClient(user.getServiceClient().toString())
                .userPlan(account.getUserPlan().toString())
                .action("CREATE")
                .build();

        //feignClientService.syncUser(event);
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public Empresa createEmpresa(EmpresaRequest request) {
        Empresa empresa = new Empresa();
        empresa.setNombre(request.getNombre());
        empresa.setDireccion(request.getDireccion());
        empresa.setEmail(request.getEmail());
        empresa.setTelefono(request.getTelefono());
        return empresaRepository.save(empresa);
    }

    public List<Empresa> getAllEmpresas() {
        return empresaRepository.findAll();
    }

    private void saveUserToken(User userDetails, String token, boolean isRefresh) {
        Token tokenAGuardar = Token.builder()
                .token(token)
                .isRefresh(isRefresh) // 👈 agrega un campo boolean en tu entidad
                .expired(false)
                .revoked(false)
                .user(userDetails)
                .build();
        tokenRepository.save(tokenAGuardar);
    }


    private void revokeAllUserTokens(User user) {
        final List<Token> validUserTokens = tokenRepository
                .findAllValidIsFalseOrRevokedIsFalseByUserId(user.getId());
        if (!validUserTokens.isEmpty()) {
            for (final Token token : validUserTokens) {
                token.setExpired(true);
                token.setRevoked(true);
            }
            tokenRepository.saveAll(validUserTokens);
        }
    }

    public AuthResponse refreshToken(final String authHeader) {
        log.info("AuthHeader recibido: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid JWT token");
        }
        final String refreshToken = authHeader.substring(7).trim();
        log.info("RefreshToken extraído: {}", refreshToken);

        final Claims claims = jwtService.getAllClaims(refreshToken);
        /*if (!"refresh".equals(claims.get("type"))) {
            throw new IllegalArgumentException("Invalid JWT token type");
        }*/

        final String userEmail = jwtService.getUsernameFromToken(refreshToken);
        if (userEmail == null) {
            throw new IllegalArgumentException("Token no tiene usuario, es inválido");
        }

        final User user = usersRepository.findByUsername(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        /*if (!jwtService.isTokenValidWBD(refreshToken, user)) {
            throw new IllegalArgumentException("Invalid JWT token");
        }*/

        final String newAccessToken = jwtService.getToken(user);
        final String newRefreshToken = jwtService.getRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, newAccessToken, false);
        saveUserToken(user, newRefreshToken, true);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }


}
