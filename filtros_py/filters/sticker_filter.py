"""
Filtro StickerFilter con Lógica de Capas Invertida.
Orden: Imagen del Usuario (completa/redimensionada) como base, 
y luego el fondo2.png PNG transparente se pega encima.
"""
import numpy as np
import time
from PIL import Image

class StickerFilter(object): 

    def __init__(self):
        self.name = "sticker"
        self.description = "Pone la foto del usuario como base y superpone el fondo2.png transparente encima."
        print(f"  {self.name.upper()} Filter - Inicializado correctamente")
    
    
    def generate_kernel(self, kernel_size):
        """Método Dummy: Este filtro no usa convolución."""
        return np.array([[1.0]], dtype=np.float32)

    
    def process_gpu(self, image, kernel, block_config, grid_config, sticker_img_path, target_x, target_y, target_w, target_h):
        """
        [CPU] Procesa la composición de capas - ORDEN INVERTIDO.
        
        Args:
            image (np.array): La foto de entrada del usuario (RGB uint8).
            sticker_img_path (str): Ruta al fondo2.png (PNG con transparencia).
            target_x, target_y, target_w, target_h: Coordenadas del diseño (ya no se usan para recorte).
        """
        start_time = time.perf_counter()
        
        # 1. Cargar el fondo2.png (PNG con transparencia)
        try:
            frame_pil = Image.open(sticker_img_path).convert("RGBA") 
            frame_w, frame_h = frame_pil.size
        except FileNotFoundError:
            raise FileNotFoundError(f"Archivo de fondo no encontrado en: {sticker_img_path}")
        
        # 2. Preparar la foto del usuario
        user_img_pil = Image.fromarray(image, mode='RGB').convert("RGBA")
        
        # 3. Redimensionar la foto del usuario al tamaño COMPLETO del frame
        # Esto hará que la foto cubra todo el fondo
        user_img_resized = user_img_pil.resize((frame_w, frame_h), Image.Resampling.LANCZOS)
        
        # --- COMPOSICIÓN DE CAPAS (ORDEN CORRECTO) ---
        
        # 4. CAPA BASE: La imagen del usuario (completa, redimensionada)
        base_pil = user_img_resized.copy()
        
        # 5. CAPA SUPERIOR: Superponer el fondo2.png transparente
        # Los personajes y elementos del PNG aparecerán encima de la foto
        base_pil.paste(frame_pil, (0, 0), frame_pil)
        
        # 6. Convertir el resultado final
        output_np = np.ascontiguousarray(np.array(base_pil.convert('RGB')), dtype=np.uint8)
        
        elapsed_time = time.perf_counter() - start_time
        print(f"    Composición de capas completada en {elapsed_time:.6f} segundos.")
        
        return output_np, elapsed_time


    def get_recommended_block_sizes(self):
        return [{"name": "CPU/PIL/NumPy", "config": (1, 1, 1)}]

    
    def get_parameters(self):
        return {
            "kernel_size": {
                "type": "int", "default": 1, "min": 1, "max": 1, "step": 1, 
                "description": "No aplica."
            },
            "sticker_img_path": {
                "type": "str", 
                "default": "filters/fondo2.png",
                "description": "Ruta a la imagen de fondo PNG con transparencia."
            },
            # Estos parámetros ya no se usan activamente, pero los dejamos por compatibilidad
            "target_x": {"type": "int", "default": 0, "description": "No usado actualmente."},
            "target_y": {"type": "int", "default": 0, "description": "No usado actualmente."},
            "target_w": {"type": "int", "default": 400, "description": "No usado actualmente."},
            "target_h": {"type": "int", "default": 600, "description": "No usado actualmente."}
        }