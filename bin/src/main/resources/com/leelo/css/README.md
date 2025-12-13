# Sistema de Diseño Minimalista - Leelo

## Estructura
- `main.css` - Importa el sistema de diseño
- `design-system.css` - Sistema completo y unificado

## Principios
1. **Minimalismo** - Solo lo esencial
2. **Consistencia** - Mismos patrones en toda la app
3. **Legibilidad** - Optimizado para lectura
4. **Limpieza** - Sin duplicación ni elementos innecesarios

## Clases Principales

### Tipografía
- `.heading-1` (24px), `.heading-2` (20px), `.heading-3` (16px)
- `.text-base` (15px), `.text-sm` (13px), `.text-xs` (11px)
- `.text-primary`, `.text-secondary`, `.text-muted`

### Espaciado
- `.space-1` (4px), `.space-2` (8px), `.space-3` (12px), `.space-4` (16px), `.space-6` (24px)
- `.pad-1` (4px), `.pad-2` (8px), `.pad-3` (12px), `.pad-4` (16px), `.pad-6` (24px)

### Botones
- `.btn` + `.btn-primary`, `.btn-secondary`, `.btn-success`, `.btn-danger`
- `.btn-sm` para botones pequeños
- `.btn-minimal` para controles discretos

### Contenedores
- `.container` - Contenedor principal
- `.card` - Tarjeta con borde y sombra
- `.max-content-width` - Ancho máximo 1000px

### Formularios
- `.form-label`, `.form-input`, `.form-textarea`, `.search-input`

### Navegación
- `.nav-container`, `.nav-btn`, `.nav-btn-active`

### Lectura (Minimalista)
- `.reading-controls-minimal` - Barra de controles compacta
- `.reading-content-clean` - Contenido limpio sin bordes
- `.reading-text-clean` - Texto optimizado para lectura
- `.word-new`, `.word-learning`, `.word-learned`, `.word-mastered`

### Utilidades
- `.center`, `.center-left`, `.center-right`
- `.full-width`, `.grow`, `.grow-h`, `.grow-v`

## Interfaz de Lectura
La interfaz de lectura ha sido completamente rediseñada para ser minimalista:
- Controles en una sola línea horizontal compacta
- Navegación con símbolos simples (‹ ›)
- Controles de fuente compactos (A⁻ A⁺)
- Leyenda visual con solo puntos de colores
- Contenido de lectura sin bordes ni distracciones
- Espaciado optimizado para la lectura

## Uso
```xml
<!-- Título -->
<Label text="Mi Título" styleClass="heading-1, text-primary" />

<!-- Botón -->
<Button text="Guardar" styleClass="btn, btn-primary" />

<!-- Contenedor con espaciado -->
<VBox styleClass="space-3, pad-4">
    <!-- Contenido -->
</VBox>
```

## Beneficios
- ✅ Interfaz limpia y minimalista
- ✅ Controles de lectura compactos
- ✅ Sin duplicación de código
- ✅ Fácil mantenimiento
- ✅ Consistencia visual total
- ✅ Optimizado para la experiencia de lectura