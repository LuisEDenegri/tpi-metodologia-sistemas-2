# Sistema de Gestión de Pedidos — Distribuidora Cuyo

Trabajo Práctico Integrador de **Metodología de Sistemas II** — Tecnicatura
Universitaria en Programación a Distancia, UTN.

Módulo de gestión de pedidos de una distribuidora de productos regionales
mendocinos. El objetivo del trabajo no es agregar funcionalidad, sino **mejorar
la calidad interna** de un sistema que ya funciona: diagnosticar deuda técnica,
aplicar refactorings seguros y verificar que el comportamiento externo se
mantiene.

## Descripción

El sistema permite registrar pedidos de clientes, controlar el stock
disponible, hacer avanzar los pedidos por sus estados, asignar un responsable y
generar un reporte para administración.

### Entidades

| Entidad | Rol |
|---|---|
| `Pedido` | Solicitud de compra: producto, cantidad, importes, estado |
| `Cliente` | Comprador, identificado por documento |
| `Producto` (por código) | Ítem del catálogo con su stock |
| `Estado` | Situación del pedido dentro de su ciclo de vida |

### Operaciones funcionales

1. **Registrar pedido** — valida stock, da de alta el cliente si es nuevo, calcula importes y descuenta stock.
2. **Cambiar estado** — hace avanzar el pedido respetando las transiciones válidas; al cancelar repone el stock.
3. **Asignar responsable** — vincula el pedido a una persona y un área.
4. **Buscar / filtrar** — por estado, provincia, monto mínimo, urgencia y producto.
5. **Reporte** — listado con totales por estado y facturación acumulada.

### Ciclo de vida del pedido

```
PENDIENTE ──► PAGADO ──► PREPARACION ──► DESPACHADO ──► ENTREGADO
    │            │
    └────────────┴──► CANCELADO   (repone stock)
```

## Tecnologías

- **Java 25** (sin dependencias externas)
- Persistencia **en memoria** mediante estructuras `Map`
- Pruebas: *runner* propio, sin framework externo

La ausencia de librerías externas es deliberada: mantiene el foco del trabajo en
el diseño y permite compilar y ejecutar con un JDK limpio.

## Instalación y uso

Requiere un JDK 17 o superior (`javac --version` para verificar).

```bash
# compilar
javac -d out -encoding UTF-8 src/pedidos/*.java

# ejecutar la demostración
java -cp out pedidos.Main
```

### Ejecutar las pruebas

```bash
javac -d out -encoding UTF-8 src/pedidos/*.java src/pedidos/pruebas/*.java
java -cp out pedidos.pruebas.CorridaDePruebas
```

## Estructura del repositorio

```
src/pedidos/          código fuente
docs/                 checklist de code review, evidencias y decisiones técnicas
README.md             este archivo
```

## Tags de estado

| Tag | Hito |
|---|---|
| `estado-inicial` | Sistema base, antes de intervenir |
| `etapa-diagnostico` | Diagnóstico de calidad y code smells documentado |
| `etapa-refactoring` | Refactorings aplicados |
| `etapa-pruebas` | Pruebas de caracterización y verificación |
| `etapa-docs` | Documentación y code review |
| `estado-refactorizado` | Estado final del código |

Para ver el contraste antes/después:

```bash
git diff estado-inicial estado-refactorizado -- src/
```

## Datos

Todos los datos de clientes, pedidos y productos son **sintéticos**. No se
utilizan datos personales reales.
