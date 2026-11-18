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

Requiere un JDK 17 o superior (`javac --version` para verificar). No hay que
instalar nada más: no usa Maven ni descarga dependencias.

```bash
# compilar (las dos sondas de diagnóstico quedan afuera a propósito, ver abajo)
javac -d out -encoding UTF-8 \
  $(find src -name "*.java" -not -name "SondaDeDefectos.java" -not -name "SondaRevision.java")

# ejecutar la demostración
java -cp out pedidos.Main
```

En PowerShell, el mismo compilado:

```powershell
$fuentes = Get-ChildItem src -Recurse -Filter *.java |
    Where-Object { $_.Name -notin 'SondaDeDefectos.java','SondaRevision.java' }
javac -d out -encoding UTF-8 $fuentes.FullName
```

### Ejecutar las pruebas

```bash
java -cp out pedidos.pruebas.CorridaDePruebas              # resumen
java -cp out pedidos.pruebas.CorridaDePruebas --detallado  # caso por caso
```

Resultado esperado: **69 casos de caracterización + 50 de comportamiento
correcto, todos en verde**, y D-03 nombrado como defecto abierto por decisión
explícita (ver `docs/decisiones-tecnicas.md`, D11).

### Verificar que los defectos no reaparecieron

```bash
java -cp out sondas.SondaDeRegresion
```

Re-ejecuta contra el sistema refactorizado los mismos escenarios que exhibían
cada defecto en el diagnóstico. Un defecto no se da por corregido porque el
refactoring "debería" haberlo arreglado, sino porque se corre el escenario y
el sistema ya no se comporta así.

## Estructura del repositorio

```
src/pedidos/          código del dominio
src/pedidos/pruebas/  runner propio y las dos suites de pruebas
src/sondas/           sondas de diagnóstico y de regresión
docs/                 checklist de code review y decisiones técnicas
docs/evidencias/      salidas de ejecución antes/después
README.md             este archivo
```

### Las dos sondas que no compilan

`SondaDeDefectos` y `SondaRevision` están escritas contra la API del **sistema
base** y se excluyen del compilado a propósito: son evidencia histórica del
estado inicial. Migrarlas a la API nueva las convertiría en otra cosa.

Su código sigue siendo compilable en el tag `estado-inicial`, y su salida está
archivada en `docs/evidencias/`. Para el estado final existe
`SondaDeRegresion`, que sí compila y corre.

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
git log --oneline estado-inicial..estado-refactorizado
```

## Resultado del trabajo

| | |
|---|---|
| Defectos confirmados en el diagnóstico | 14 |
| Corregidos y verificados por ejecución | 13 |
| Abiertos por decisión explícita | 1 (D-03, ver `docs/decisiones-tecnicas.md`) |
| Hipótesis descartadas, sin regresión | 1 |
| Casos de prueba | 119 |

El code review posterior al refactoring levantó 13 observaciones y las 13 se
reprodujeron ejecutando código: cero falsos positivos. **Tres de los cuatro
hallazgos críticos son defectos que el propio refactoring introdujo o
reabrió** mientras corregía otros. Está desarrollado en
`docs/checklist-code-review.md`.

## Datos

Todos los datos de clientes, pedidos y productos son **sintéticos**. No se
utilizan datos personales reales.
