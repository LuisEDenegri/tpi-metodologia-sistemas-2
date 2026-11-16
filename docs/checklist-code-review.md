# Checklist de code review

Etapa 7 del TPI. Este checklist se aplicó sobre el código **ya refactorizado**,
no sobre el sistema base. Esa distinción importa: el diagnóstico inicial
(etapa 3) buscaba deuda técnica heredada; esta revisión buscó deuda técnica
**introducida por nuestro propio trabajo**.

## Cómo se usó

1. Se recorrió el repositorio clase por clase contra los ítems de abajo.
2. Cada hallazgo se anotó con severidad **antes** de intentar arreglarlo.
3. **Ningún hallazgo se aceptó por plausible.** Antes de tocar código, se
   escribió un escenario que lo reprodujera y se ejecutó. El criterio fue:
   si no se puede exhibir corriendo, no es un defecto, es una opinión.
4. Cada hallazgo confirmado se corrigió y se le dejó un caso de prueba que lo
   vigile.

De 13 observaciones levantadas, las 13 se reprodujeron. **Cero falsos
positivos.** Eso no habla bien de la revisión: habla mal del código que
revisamos, que era el nuestro.

## Criterios de aceptación técnica

Un cambio se considera aceptado cuando cumple los cinco:

| # | Criterio | Cómo se verifica |
|---|---|---|
| A-1 | La suite completa queda en verde | `java -cp out pedidos.pruebas.CorridaDePruebas` |
| A-2 | El defecto corregido tiene un caso que lo vigila | el caso falla si se revierte el fix |
| A-3 | Ninguna prueba de caracterización se tocó para que pase | `git diff` sobre `CaracterizacionBase` justificado en el commit |
| A-4 | El comportamiento externo no cambió salvo donde se declaró | comparar `ejecucion-ANTES.txt` / `ejecucion-DESPUES.txt` |
| A-5 | El commit explica el *porqué*, no el *qué* | revisión del mensaje |

## Checklist

### 1. Responsabilidades y estructura

- [x] Cada clase tiene una sola razón para cambiar (SRP)
- [x] Ningún método supera ~20 líneas
- [x] Ningún método recibe más de 3 parámetros sin objeto parámetro
- [x] No hay God Class
- [x] La lógica de negocio vive en el objeto que conoce la regla, no en el coordinador
- [x] Presentación separada de gestión

### 2. Nombres

- [x] El nombre del método dice lo que el método hace
      → **hallazgo S-3**: `cargarStock` reemplazaba en vez de sumar. Partido en `ingresar` / `fijar`.
- [x] No quedan nombres que mienten o que son ambiguos
- [x] Los booleanos se leen como afirmaciones
      → **hallazgo S-2**: `Boolean urgente` admitía tres valores y tenía una rama muerta. Pasa a `boolean soloUrgentes`.
- [x] Las constantes mágicas tienen nombre

### 3. Encapsulamiento

- [x] No hay campos públicos mutables
- [x] Los getters de colecciones no devuelven la colección interna
      → **hallazgo C-3**: `buscar()` le pasaba el `LinkedHashMap` vivo al filtro, que es un punto de extensión público. Una subclase podía vaciar la base de clientes con un `clear()`.
      → **hallazgo I-2**: `stock()` devolvía copia mutable mientras `pedidos()` y `clientes()` devolvían vistas inmutables. Contratos distintos para lo mismo.
- [x] Los mutadores del dominio no son alcanzables desde fuera del paquete
      → **hallazgo C-2**: `cambiarEstadoA` y `asignarResponsable` eran públicos y `getPedido()` entrega el `Pedido` vivo. Se podía cancelar un pedido por afuera del manager y el inventario no reponía.
- [x] **Verificado por el compilador, no por runtime.** Ver `evidencias/encapsulamiento-compilador.txt`: código cliente fuera del paquete que intente mutar el dominio no compila.

### 4. Estado e inmutabilidad

- [x] Los objetos históricos son inmutables
      → **hallazgo I-1**: el `Pedido` no guardaba la dirección con la que se cotizó; había que ir a buscarla al `Cliente`, que ahora es mutable. Un cliente que pedía a Mendoza y después a Córdoba dejaba el primer pedido facturado con flete de Mendoza y listado como de Córdoba.
- [x] Las actualizaciones parciales no borran lo que no traen
      → **hallazgo C-1**: al corregir "los datos del cliente quedan congelados", la actualización pasó a ser incondicional. Un pedido válido sin mail **vaciaba** el mail bueno del cliente, que dejaba de recibir toda notificación futura.
- [x] Las colecciones internas se inicializan en la declaración

### 5. Consistencia de reglas

- [x] La misma regla se aplica igual en todos los puntos del sistema
      → **hallazgo C-4**: `ZonaEnvio` normaliza la provincia (`trim` + ignore case) y el filtro de búsqueda comparaba con `equals`. Un pedido cargado como `"mendoza"` se cobraba con tarifa de Mendoza pero no aparecía al buscar `"Mendoza"`: se cobraba según una regla y se listaba según otra.
- [x] Dos implementaciones de una interfaz son observablemente sustituibles (LSP)
      → **hallazgo I-6**: `NotificadorPorEmail.notificarStockBajo` registraba además el evento `STOCK_BAJO`, cosa que `NotificadorSilencioso` no hacía. El evento existía en producción y era invisible desde las pruebas. La decisión sube al coordinador.

### 6. Errores y validación

- [x] No hay `catch` vacíos
- [x] Una falla de notificación no voltea la operación de negocio, pero tampoco desaparece
- [x] La validación cubre todos los parámetros, no solo el obvio
      → **hallazgo I-3**: `Inventario` validaba la cantidad y dejaba el código afuera. Entraba `{null=5}`: una entrada que ningún pedido iba a referenciar nunca.
- [x] Los mensajes de error dicen qué valor llegó y qué se esperaba
- [x] La capacidad de diagnóstico construida no se descarta en la fachada
      → **hallazgo I-5**: `SolicitudDePedido` sabía explicar con precisión por qué una solicitud era inválida, y `registrar()` tiraba ese mensaje y devolvía `null` a secas. Se expone `motivoDeRechazo()`.

### 7. Tipos de dominio

- [x] El dinero no se representa en `double`
      → **hallazgo S-5**: `Dinero` encapsulaba bien, pero exponía `valor()` devolviendo el `double` crudo: la puerta de atrás para volver a sumar plata con doubles.
- [x] Los códigos de tipo son enums, no Strings
- [x] Los conjuntos de datos que viajan juntos son un objeto

### 8. Apertura a la extensión

- [x] Agregar un caso nuevo no obliga a tocar condicionales existentes (OCP)
      → **hallazgo S-4**: el reporte imprimía seis líneas escritas a mano mientras contaba recorriendo `values()`. Un estado nuevo en el enum se contaba pero **no se imprimía**, sin ningún error que lo delatara. La etiqueta pasa al enum.
- [x] Las dependencias apuntan a abstracciones (DIP)

### 9. Pruebas

- [x] Existe red de caracterización antes de refactorizar
- [x] Un defecto que se decide **no** corregir queda congelado, no escondido
      → D-03 se congela con `v.igual` y no con `v.yaNoOcurre`. La caracterización documenta la verdad: el defecto sigue vivo. Si algún día se implementan reservas de stock, ese caso se pone en rojo y **ese rojo es la señal de que se corrigió**.
- [x] Las aserciones no tienen escapes que las hagan pasar de todas formas
      → **autohallazgo**: un caso afirmaba `stockDe(...) >= 0`, que pasa aunque la mutación externa tenga éxito. Se cambió por comparación contra el valor esperado capturado antes.
- [x] La corrida en verde no afirma más de lo que verificó
      → **autohallazgo**: la suite decía "sin defectos pendientes" con D-03 abierto. Ahora lo nombra siempre.

### 10. Repositorio

- [x] El historial de commits se lee como una narración del trabajo
- [x] Los tags de etapa existen y apuntan al commit correcto
- [x] El README describe cómo compilar y correr **lo que hay hoy**
- [x] Las evidencias antes/después están versionadas, no sueltas

## Resultado

| Severidad | Levantados | Confirmados | Corregidos |
|---|---|---|---|
| Crítico | 4 | 4 | 4 |
| Importante | 6 | 6 | 6 |
| Sugerencia | 3 | 3 | 3 |

El hallazgo más incómodo, y el más útil para el trabajo: **tres de los cuatro
críticos son defectos que el refactoring introdujo o reabrió mientras
corregía otros.** I-1 es literalmente el defecto "se cobra un destino y se
envía a otro" del sistema base, reaparecido con el signo invertido.

Esa es la contracara honesta de las 112 pruebas en verde: refactorizar con red
de pruebas **reduce** el riesgo, no lo elimina. La red cubre lo que alguien
pensó en cubrir. Un code review independiente encuentra lo que la red no
pensó.
