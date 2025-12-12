import numpy as np
import pycuda.driver as cuda
from pycuda.compiler import SourceModule


class BlurFilter:

    def __init__(self):
        self.name = "blur"
        self.description = "Aplica desenfoque gaussiano a la imagen"

        self.kernel_code = """
        __global__ void blur_kernel(float *input, float *output, float *kernel, 
                                    int width, int height, int channels, int kernel_size)
        {
            int x = blockIdx.x * blockDim.x + threadIdx.x;
            int y = blockIdx.y * blockDim.y + threadIdx.y;
            
            if (x >= width || y >= height) return;
            
            int r = kernel_size / 2;
            
            // Procesar cada canal (R, G, B)
            for (int c = 0; c < channels; c++) {
                float sum = 0.0f;
                
                // Convolución gaussiana
                for (int ky = -r; ky <= r; ky++) {
                    int yy = y + ky;
                    if (yy < 0 || yy >= height) continue;
                    
                    for (int kx = -r; kx <= r; kx++) {
                        int xx = x + kx;
                        if (xx < 0 || xx >= width) continue;
                        
                        // Índice en memoria: [y][x][c]
                        int pixel_idx = (yy * width + xx) * channels + c;
                        float pixel_val = input[pixel_idx];
                        float kernel_val = kernel[(ky + r) * kernel_size + (kx + r)];
                        sum += pixel_val * kernel_val;
                    }
                }
                
                // Escribir resultado para este canal
                int out_idx = (y * width + x) * channels + c;
                output[out_idx] = sum;
            }
        }
        """
        
        self.module = SourceModule(self.kernel_code)
        self.cuda_function = self.module.get_function("blur_kernel")
    
    def generate_kernel(self, kernel_size, sigma=1.0):
        """Genera un kernel gaussiano normalizado"""
        
        # Asegurar que kernel_size sea impar
        if kernel_size % 2 == 0:
            kernel_size += 1
        
        kernel = np.zeros((kernel_size, kernel_size), dtype=np.float32)
        r = kernel_size // 2
        
        # Generar kernel gaussiano 2D
        for i in range(kernel_size):
            for j in range(kernel_size):
                y = i - r
                x = j - r
                
                # Fórmula gaussiana 2D: G(x,y) = (1/(2πσ²)) * e^(-(x²+y²)/(2σ²))
                exponent = -(x*x + y*y) / (2.0 * sigma * sigma)
                kernel[i, j] = np.exp(exponent)
        
        # Normalizar el kernel (suma = 1)
        kernel_sum = np.sum(kernel)
        if kernel_sum > 0:
            kernel /= kernel_sum
        
        return kernel
    
    def process_gpu(self, image, kernel, block_config, grid_config):
        """Procesa la imagen con blur gaussiano en GPU (soporta escala de grises y RGB)"""
        
        # Determinar dimensiones
        if len(image.shape) == 3:
            H, W, C = image.shape
            # Convertir RGB uint8 a float32 para procesamiento
            image_float = image.astype(np.float32)
        else:
            H, W = image.shape
            C = 1
            # Ya está en float32 si es escala de grises
            image_float = image
        
        K = kernel.shape[0]

        # Allocar memoria GPU
        input_gpu = cuda.mem_alloc(image_float.nbytes)
        output_gpu = cuda.mem_alloc(image_float.nbytes)
        kernel_gpu = cuda.mem_alloc(kernel.nbytes)

        # Copiar datos a GPU
        cuda.memcpy_htod(input_gpu, image_float)
        cuda.memcpy_htod(kernel_gpu, kernel.flatten())

        # Medir tiempo
        start = cuda.Event()
        end = cuda.Event()
        start.record()

        # Ejecutar kernel CUDA
        self.cuda_function(
            input_gpu, output_gpu, kernel_gpu,
            np.int32(W), np.int32(H), np.int32(C), np.int32(K),
            block=block_config, grid=grid_config
        )

        end.record()
        end.synchronize()
        gpu_time = start.time_till(end)

        # Copiar resultado de vuelta
        output = np.empty_like(image_float)
        cuda.memcpy_dtoh(output, output_gpu)

        # Liberar memoria GPU
        input_gpu.free()
        output_gpu.free()
        kernel_gpu.free()

        # Si era RGB, convertir de vuelta a uint8
        if len(image.shape) == 3:
            output = np.clip(output, 0, 255).astype(np.uint8)

        return output, gpu_time
    
    def get_recommended_block_sizes(self):
        return [
            {"name": "8x8", "config": (8, 8, 1)},
            {"name": "16x16", "config": (16, 16, 1)},
            {"name": "32x32", "config": (32, 32, 1)},
        ]
    
    def get_parameters(self):
        return {
            "kernel_size": {
                "type": "int",
                "default": 5,
                "min": 3,
                "max": 31,
                "description": "Tamaño del kernel gaussiano (debe ser impar)"
            },
            "sigma": {
                "type": "float",
                "default": 1.0,
                "min": 0.1,
                "max": 10.0,
                "description": "Desviación estándar de la gaussiana"
            }
        }