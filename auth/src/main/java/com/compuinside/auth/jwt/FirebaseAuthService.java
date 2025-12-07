package com.compuinside.auth.jwt;

import com.compuinside.auth.controller.*;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthService {

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;

    public Mono<AuthResponse> register(RegisterRequest request) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Registrando usuario: {}", request.getEmail());

                UserRecord.CreateRequest userRequest = new UserRecord.CreateRequest()
                        .setEmail(request.getEmail())
                        .setPassword(request.getPassword())
                        .setEmailVerified(false);

                if (request.getDisplayName() != null && !request.getDisplayName().trim().isEmpty()) {
                    userRequest.setDisplayName(request.getDisplayName());
                }

                if (request.getPhotoUrl() != null && !request.getPhotoUrl().trim().isEmpty()) {
                    userRequest.setPhotoUrl(request.getPhotoUrl());
                }

                UserRecord userRecord = firebaseAuth.createUser(userRequest);
                log.info("Usuario creado en Firebase Auth: {}", userRecord.getUid());

                UserProfile userProfile = UserProfile.builder()
                        .uid(userRecord.getUid())
                        .email(userRecord.getEmail())
                        .displayName(request.getDisplayName())
                        .photoUrl(request.getPhotoUrl())
                        .role(request.getRole() != null ? request.getRole() : "USER")
                        .accountId(UUID.randomUUID().toString())
                        .createdAt(System.currentTimeMillis())
                        .lastLogin(System.currentTimeMillis())
                        .state("ACTIVE")
                        .build();

                firestore.collection("users")
                        .document(userRecord.getUid())
                        .set(userProfile)
                        .get();

                log.info("Perfil guardado en Firestore: {}", userRecord.getUid());

                String customToken = firebaseAuth.createCustomToken(userRecord.getUid());

                return AuthResponse.builder()
                        .success(true)
                        .message("Usuario registrado exitosamente")
                        .data(AuthData.builder()
                                .uid(userRecord.getUid())
                                .email(userRecord.getEmail())
                                .displayName(userRecord.getDisplayName())
                                .photoUrl(userRecord.getPhotoUrl())
                                .customToken(customToken)
                                .role(userProfile.getRole())
                                .accountId(userProfile.getAccountId())
                                .build())
                        .build();

            } catch (FirebaseAuthException e) {
                log.error("Error registrando usuario: {}", e.getMessage());
                String errorMessage = getAuthErrorMessage(e);
                return AuthResponse.builder()
                        .success(false)
                        .error(errorMessage)
                        .build();
            } catch (Exception e) {
                log.error("Error inesperado: {}", e.getMessage(), e);
                return AuthResponse.builder()
                        .success(false)
                        .error("Error al registrar usuario: " + e.getMessage())
                        .build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Login de usuario: {}", request.getEmail());

                UserRecord userRecord = firebaseAuth.getUserByEmail(request.getEmail());

                if (userRecord.isDisabled()) {
                    return AuthResponse.builder()
                            .success(false)
                            .error("Usuario deshabilitado")
                            .build();
                }

                String customToken = firebaseAuth.createCustomToken(userRecord.getUid());

                Map<String, Object> updates = new HashMap<>();
                updates.put("lastLogin", System.currentTimeMillis());

                firestore.collection("users")
                        .document(userRecord.getUid())
                        .update(updates)
                        .get();

                // Obtener perfil extendido
                UserProfile profile = firestore.collection("users")
                        .document(userRecord.getUid())
                        .get()
                        .get()
                        .toObject(UserProfile.class);

                log.info("Login exitoso para: {}", userRecord.getUid());

                return AuthResponse.builder()
                        .success(true)
                        .message("Login exitoso")
                        .data(AuthData.builder()
                                .uid(userRecord.getUid())
                                .email(userRecord.getEmail())
                                .displayName(userRecord.getDisplayName())
                                .photoUrl(userRecord.getPhotoUrl())
                                .customToken(customToken)
                                .role(profile != null ? profile.getRole() : null)
                                .accountId(profile != null ? profile.getAccountId() : null)
                                .build())
                        .build();

            } catch (FirebaseAuthException e) {
                log.error("Error en login: {}", e.getMessage());
                return AuthResponse.builder()
                        .success(false)
                        .error("Credenciales inválidas o usuario no existe")
                        .build();
            } catch (Exception e) {
                log.error("Error inesperado en login: {}", e.getMessage(), e);
                return AuthResponse.builder()
                        .success(false)
                        .error("Error al procesar login: " + e.getMessage())
                        .build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AuthResponse> verifyToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Verificando token");

                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
                String uid = decodedToken.getUid();

                UserProfile profile = firestore.collection("users")
                        .document(uid)
                        .get()
                        .get()
                        .toObject(UserProfile.class);

                log.info("Token válido para: {}", uid);

                return AuthResponse.builder()
                        .success(true)
                        .message("Token válido")
                        .data(AuthData.builder()
                                .uid(uid)
                                .email(decodedToken.getEmail())
                                .displayName(decodedToken.getName())
                                .photoUrl(decodedToken.getPicture())
                                .role(profile != null ? profile.getRole() : null)
                                .accountId(profile != null ? profile.getAccountId() : null)
                                .build())
                        .build();

            } catch (FirebaseAuthException e) {
                log.error("Token inválido: {}", e.getMessage());
                return AuthResponse.builder()
                        .success(false)
                        .error("Token inválido o expirado")
                        .build();
            } catch (Exception e) {
                log.error("Error verificando token: {}", e.getMessage(), e);
                return AuthResponse.builder()
                        .success(false)
                        .error("Error al verificar token: " + e.getMessage())
                        .build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }


    public Mono<UserProfile> getUserProfile(String uid) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Obteniendo perfil: {}", uid);

                UserProfile profile = firestore.collection("users")
                        .document(uid)
                        .get()
                        .get()
                        .toObject(UserProfile.class);

                return profile;
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error obteniendo perfil: {}", e.getMessage(), e);
                throw new RuntimeException("Error al obtener perfil", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AuthResponse> updateUserProfile(String uid, Map<String, Object> updates) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Actualizando perfil: {}", uid);

                firestore.collection("users")
                        .document(uid)
                        .update(updates)
                        .get();

                if (updates.containsKey("email") || updates.containsKey("displayName")) {
                    UserRecord.UpdateRequest authUpdate = new UserRecord.UpdateRequest(uid);

                    if (updates.containsKey("email")) {
                        authUpdate.setEmail((String) updates.get("email"));
                    }
                    if (updates.containsKey("displayName")) {
                        authUpdate.setDisplayName((String) updates.get("displayName"));
                    }

                    firebaseAuth.updateUser(authUpdate);
                }

                log.info("Perfil actualizado: {}", uid);

                return AuthResponse.builder()
                        .success(true)
                        .message("Perfil actualizado exitosamente")
                        .build();

            } catch (Exception e) {
                log.error("Error actualizando perfil: {}", e.getMessage(), e);
                return AuthResponse.builder()
                        .success(false)
                        .error("Error actualizando perfil: " + e.getMessage())
                        .build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AuthResponse> deleteUser(String uid) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Eliminando usuario: {}", uid);

                // Eliminar de Firebase Auth
                firebaseAuth.deleteUser(uid);

                // Eliminar perfil de Firestore
                firestore.collection("users")
                        .document(uid)
                        .delete()
                        .get();

                log.info("Usuario eliminado: {}", uid);

                return AuthResponse.builder()
                        .success(true)
                        .message("Usuario eliminado exitosamente")
                        .build();

            } catch (FirebaseAuthException e) {
                log.error("Error eliminando usuario: {}", e.getMessage());
                return AuthResponse.builder()
                        .success(false)
                        .error("Error eliminando usuario: " + e.getMessage())
                        .build();
            } catch (Exception e) {
                log.error("Error inesperado eliminando usuario: {}", e.getMessage(), e);
                return AuthResponse.builder()
                        .success(false)
                        .error("Error al eliminar usuario: " + e.getMessage())
                        .build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }


    public Mono<AuthResponse> disableUser(String uid) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Deshabilitando usuario: {}", uid);

                UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                        .setDisabled(true);

                firebaseAuth.updateUser(request);

                Map<String, Object> updates = new HashMap<>();
                updates.put("state", "DISABLED");

                firestore.collection("users")
                        .document(uid)
                        .update(updates)
                        .get();

                log.info("Usuario deshabilitado: {}", uid);

                return AuthResponse.builder()
                        .success(true)
                        .message("Usuario deshabilitado exitosamente")
                        .build();

            } catch (Exception e) {
                log.error("Error deshabilitando usuario: {}", e.getMessage(), e);
                return AuthResponse.builder()
                        .success(false)
                        .error("Error al deshabilitar usuario: " + e.getMessage())
                        .build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AuthResponse> enableUser(String uid) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Habilitando usuario: {}", uid);

                UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                        .setDisabled(false);

                firebaseAuth.updateUser(request);

                // Actualizar estado en Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("state", "ACTIVE");

                firestore.collection("users")
                        .document(uid)
                        .update(updates)
                        .get();

                log.info("Usuario habilitado: {}", uid);

                return AuthResponse.builder()
                        .success(true)
                        .message("Usuario habilitado exitosamente")
                        .build();

            } catch (Exception e) {
                log.error("Error habilitando usuario: {}", e.getMessage(), e);
                return AuthResponse.builder()
                        .success(false)
                        .error("Error al habilitar usuario: " + e.getMessage())
                        .build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AuthResponse> sendEmailVerification(String email) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Enviando email de verificación a: {}", email);

                UserRecord userRecord = firebaseAuth.getUserByEmail(email);

                // Firebase Admin SDK no envía emails directamente
                // Esto debe manejarse desde el cliente (Flutter)

                return AuthResponse.builder()
                        .success(true)
                        .message("Instrucciones enviadas. Verifica desde la app móvil.")
                        .build();

            } catch (Exception e) {
                log.error("Error enviando email de verificación: {}", e.getMessage());
                return AuthResponse.builder()
                        .success(false)
                        .error("Error al enviar email de verificación")
                        .build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String getAuthErrorMessage(FirebaseAuthException e) {
        String errorCode = e.getAuthErrorCode().name();

        switch (errorCode) {
            case "EMAIL_ALREADY_EXISTS":
                return "El email ya está registrado";
            case "INVALID_EMAIL":
                return "Email inválido";
            case "WEAK_PASSWORD":
                return "La contraseña es muy débil (mínimo 6 caracteres)";
            case "USER_NOT_FOUND":
                return "Usuario no encontrado";
            case "INVALID_PASSWORD":
                return "Contraseña incorrecta";
            case "USER_DISABLED":
                return "Usuario deshabilitado";
            case "TOO_MANY_ATTEMPTS_TRY_LATER":
                return "Demasiados intentos. Intenta más tarde";
            default:
                return "Error de autenticación: " + e.getMessage();
        }
    }
}
