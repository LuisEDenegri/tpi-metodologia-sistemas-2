# Decisiones técnicas

Las decisiones que no se leen en el código, y por qué se tomaron. Cada una
tuvo alternativa; acá queda asentado qué se descartó y a cambio de qué.

---

## D1 — Java plano, sin Maven, sin JUnit, sin librerías

**Decisión.** El proyecto compila con `javac` y un JDK limpio. Las pruebas
corren con un *runner* propio de unas 60 líneas.

**Alternativa descartada.** Maven + JUnit 5 es lo que se usaría en un trabajo
real.

**Por qué.** El TPI se evalúa por la calidad del análisis, no por el
andamiaje. Un `pom.xml` con dependencias resueltas desde internet agrega un
punto de falla el día de la corrección ("los enlaces deben estar accesibles al
momento de la corrección") y no aporta nada a lo que se evalúa. Que la suite
corra con dos comandos y sin red es una ventaja concreta.

**Qué se pierde.** Sin JUnit no hay `@BeforeEach`, ni *parameterized tests*,
ni reporte XML para CI. El runner propio nombra cada caso y cuenta pasadas y
fallidas, que es el 80% de lo que se usa.

**Cuándo cambiar.** Si el proyecto suma un segundo módulo o entra a un
pipeline de CI, Maven deja de ser overhead y pasa a ser necesario.

---

## D2 — Pruebas de caracterización antes que correcciones

**Decisión.** Primero se escribió `CaracterizacionBase`, que fija el
comportamiento **actual del sistema base, defectos incluidos**. Recién después
se tocó una línea de producción.

**Por qué.** Es la técnica de Michael Feathers para código heredado sin
pruebas. Sin esa red, "lo refactoricé y sigue andando" es una afirmación sin
respaldo: no hay forma de distinguir *no rompí nada* de *rompí algo que nadie
miró*.

**La consecuencia incómoda, aceptada a propósito.** La caracterización fija
**bugs**. Un caso dice, textualmente, que cancelar después de un recuento
físico inventa unidades de stock. Eso no es un error de la prueba: es la
prueba haciendo su trabajo. Cuando un defecto se corrige deliberadamente, el
caso se **actualiza para registrar el cambio**, y el commit explica por qué.
Lo que nunca se hace es editarlo para que pase.

---

## D3 — Dos suites separadas, no una

**Decisión.** `CaracterizacionBase` (62 casos) y `ComportamientoCorrecto` (50
casos) son suites distintas con semánticas opuestas.

| Suite | Arranca | Un rojo significa |
|---|---|---|
| Caracterización | verde | **regresión**: rompimos algo |
| Comportamiento correcto | roja | defecto todavía sin corregir |

**Por qué.** Mezcladas, el número total no dice nada. Separadas, cada corrida
responde dos preguntas distintas: *¿rompí algo?* y *¿cuánto llevo
corregido?*. Es la diferencia entre refactoring (cambia estructura, no
comportamiento) y corrección de defectos (cambia comportamiento a propósito).
Confundirlas es lo que hace que "se rompió algo" sea indistinguible de "se
arregló algo".

---

## D4 — `BigDecimal` encapsulado en `Dinero`, no `BigDecimal` suelto

**Decisión.** Existe un tipo `Dinero` con `mas()`, `por()` y `toString()` en
formato argentino. `BigDecimal` no aparece en ninguna firma pública.

**Alternativa descartada.** Usar `BigDecimal` directamente en todas las
clases.

**Por qué.** `BigDecimal` resuelve la aritmética pero no impide que alguien
olvide el `setScale(2, HALF_UP)` en una suma, o que compare con `equals` en
vez de `compareTo` (`2.0` y `2.00` no son `equals`). El tipo de dominio pone
esas decisiones en un solo lugar.

**Consecuencia.** `Dinero.valor()`, que devolvía el `double` crudo, se
eliminó en la revisión: era la puerta de atrás que dejaba volver a sumar plata
con doubles.

---

## D5 — Encapsulamiento por visibilidad de paquete, no por validación en runtime

**Decisión.** `Pedido.cambiarEstadoA`, `Pedido.asignarResponsable` y
`Cliente.actualizarDatosDeContacto` son *package-private*.

**Alternativa descartada.** Dejarlos públicos y validar en el cuerpo del
método que la llamada venga de un lugar autorizado.

**Por qué.** Una validación en runtime se puede saltear, está sujeta a que
alguien la escriba bien, y falla tarde: cuando el sistema ya corre. La
visibilidad de paquete la hace cumplir el **compilador**. Código cliente fuera
del paquete que intente mutar el dominio no llega a ejecutarse porque no llega
a compilar.

Eso está archivado en `evidencias/encapsulamiento-compilador.txt`: tres
errores de `javac` sobre una clase que intenta el ataque desde afuera. Un
error de compilación es la forma más fuerte de verificación de la que
disponemos acá.

**Detalle que casi nos engaña.** La primera verificación de este punto la
hicimos con una clase que declaraba `package pedidos`. Compilaba, claro. Tuvo
que escribirse una clase genuinamente externa para que el agujero quedara
realmente cerrado y demostrado.

---

## D6 — Copia defensiva **y** envoltorio inmutable para el stock

**Decisión.** `Inventario.comoMapa()` devuelve
`Collections.unmodifiableMap(new LinkedHashMap<>(interno))`. Los dos
mecanismos, no uno.

**Por qué son distintos.**

- La **copia** protege el estado real: aunque alguien escriba, el inventario
  no se toca.
- El **envoltorio** hace que esa escritura **falle ruidosamente** en vez de
  aceptarse en silencio sobre una copia que nadie va a leer.

Antes devolvía solo la copia. Quien hacía `put()` creía haber cambiado el
stock y se enteraba tarde, o nunca. Un fallo silencioso es peor que una
excepción.

---

## D7 — Transiciones de estado declaradas en el enum

**Decisión.** Cada constante de `EstadoPedido` declara sus sucesores en un
bloque `static`. `puedeTransicionarA` es un `contains`.

**Alternativa descartada.** Mantener la cadena de `if/else` anidados del
sistema base, tipada con enum en vez de String.

**Por qué.** El ciclo de vida se lee de un vistazo en seis líneas, y agregar
un estado es agregar una constante, no auditar un bloque de condicionales
(*Shotgun Surgery*). Como efecto lateral, `CANCELADO` es terminal **por
definición del enum**, no por el `else` final de un `if`: la protección contra
cancelar dos veces dejó de depender de una rama que alguien podía reordenar.

**Detalle de implementación.** Las transiciones van en un bloque `static`
posterior al constructor porque Java no permite que una constante de enum se
referencie a sí misma ni a otra que todavía no existe durante la construcción.

---

## D8 — El `Pedido` guarda su propia dirección de entrega

**Decisión.** `Pedido` tiene un campo `final direccionDeEntrega`, copiado de
la solicitud al crearse.

**Por qué.** Al corregir "los datos del cliente quedan congelados", el
`Cliente` pasó a ser mutable. Pero el pedido iba a buscar la provincia al
cliente. Un cliente que pedía a Mendoza y después a Córdoba dejaba el primer
pedido **facturado con flete de Mendoza y listado como de Córdoba**.

Es exactamente el defecto "se cobra un destino y se envía a otro" del sistema
base, reaparecido con el signo invertido, introducido por nuestro propio
refactoring. Un pedido histórico es inmutable y su destino forma parte de él.

---

## D9 — Actualización parcial que no borra

**Decisión.** `Cliente.actualizarDatosDeContacto` ignora los campos vacíos o
nulos: *un pedido actualiza los datos que trae; no borra los que no trae.*

**Alternativa descartada.** Exigir nombre, mail y teléfono en la
`SolicitudDePedido`.

**Por qué.** Exigirlos rechazaría pedidos legítimos tomados por teléfono o en
el mostrador, donde el cliente no siempre deja mail. **Entre perder un dato y
perder una venta, se conserva el dato.** La alternativa cambia una regla de
negocio para resolver un problema técnico, y esa no es la decisión de quien
escribe el código.

---

## D10 — El evento `STOCK_BAJO` lo registra el coordinador

**Decisión.** `registrarEvento("STOCK_BAJO", ...)` vive en `PedidoManager`, no
dentro de `NotificadorPorEmail`.

**Por qué (LSP).** Estaba sólo en la implementación de mail, así que
`NotificadorSilencioso` no lo emitía. Las dos implementaciones de la misma
interfaz tenían comportamiento observable distinto: el evento existía en
producción y era **invisible desde las pruebas**, que es justamente lo que el
notificador silencioso viene a evitar.

La interfaz nunca dijo si avisar implica registrar un evento, y cada
implementación contestó distinto. La decisión sube al coordinador, que es
donde están los demás `registrarEvento` y donde la secuencia completa se lee
en un solo lugar.

---

## D11 — D-03 se deja abierto y documentado

**Decisión.** Cancelar un pedido después de un recuento físico de depósito
sigue inventando unidades de stock. No se corrigió.

**Por qué.** La causa de fondo es que el sistema **no modela stock
reservado**. La mercadería de un pedido cancelado nunca salió del depósito:
ya estaba contada en el recuento. Arreglarlo de verdad exige introducir la
reserva como concepto del dominio, con su ciclo de vida propio, y eso excede
el alcance del trabajo.

**Por qué no se parchó igual.** Un parche —por ejemplo, no reponer si hubo un
`fijar` posterior— tapa el síntoma en un escenario y deja el modelo igual de
roto para los otros. Preferimos un defecto conocido, acotado y escrito a un
arreglo que da una falsa sensación de resuelto.

**Lo único que sí mejoró.** La operación ambigua `cargarStock` se partió en
`ingresarStock` y `fijarStock`, así que el recuento quedó **declarado** en vez
de ser un efecto lateral de un método cuyo nombre decía otra cosa. Eso lo hace
diagnosticable, no lo corrige.

**Un defecto atenuado no es un defecto corregido.** El titular honesto del
trabajo es **13 de 14**, no 14 de 14. La suite lo nombra en cada corrida
verde, y `CaracterizacionBase.defectoQueSigueAbierto()` lo congela con
`v.igual`: si algún día se implementan reservas, ese caso se pone en rojo, y
ese rojo es la señal de que se corrigió.

---

## D12 — Las sondas de diagnóstico quedan congeladas, no se migran

**Decisión.** `SondaDeDefectos` y `SondaRevision` siguen escritas contra la
API del sistema base y están **excluidas de la compilación**.

**Por qué.** Son evidencia histórica del estado inicial. Migrarlas a la API
nueva las convertiría en otra cosa: dejarían de demostrar cómo se comportaba
el sistema base. Su salida está archivada en `docs/evidencias/` y el código
sigue siendo compilable en el tag `estado-inicial`.

Para el estado final existe `SondaDeRegresion`, que re-ejecuta los mismos
escenarios contra el sistema refactorizado. Un defecto no se declara corregido
porque el refactoring "debería" haberlo arreglado, sino porque se corre el
escenario que lo exhibía y el sistema ya no se comporta así.

---

## D13 — `PENDIENTE` no cuenta como facturado

**Decisión.** `cuentaComoFacturado()` es verdadero desde `PAGADO` en
adelante.

**Alternativa descartada.** Mantener el criterio del sistema base: "todo lo
que no está `CANCELADO`".

**Por qué.** Un pedido recién ingresado que nadie pagó figuraba como
facturación. Si el reporte se usa para proyección de ingresos, el número está
inflado.

**Advertencia.** Esta es la única decisión del trabajo que cambia una **regla
de negocio**, no una estructura. Se asumió en el contexto académico. En un
sistema real habría que confirmarla con el área contable antes de tocarla:
puede ser que el criterio viejo fuera intencional y nosotros estemos rompiendo
un reporte que alguien usa. Queda explícita en el enum, donde se puede
discutir, en vez de implícita en una negación dentro de un `for`.

---

## Límites conocidos

Cosas que el sistema no hace y se decidió que no haga en este trabajo:

- **No modela stock reservado** (ver D11). Es el límite del que cuelga el
  único defecto abierto.
- **No persiste**. Todo vive en memoria. La consigna admite "memoria
  estructurada".
- **No modela lotes ni vencimientos.** El stock es un entero por código, igual
  que en el sistema base. La extracción de `Inventario` deja ese cambio
  contenido en una sola clase el día que el negocio lo pida.
- **No hay concurrencia.** Un solo hilo, `LinkedHashMap` sin sincronizar.
- **El catálogo de precios está en código**, no en una fuente externa.

Los puntos marcados con `ponytail:` en el código son exactamente estos: una
simplificación deliberada con su camino de actualización anotado al lado.
