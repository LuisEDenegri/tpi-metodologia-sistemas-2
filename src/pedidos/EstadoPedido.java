package pedidos;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Estado de un pedido dentro de su ciclo de vida.
 *
 * REFACTORING: Replace Type Code with State/Strategy + Replace Conditional with
 * Polymorphism (Fowler).
 *
 * Antes el estado era un String y el ciclo de vida vivia en una cadena de
 * if/else anidados de 45 lineas dentro de cambiarEstado(). Eso tenia tres
 * consecuencias:
 *
 *   1. Un typo ("PAGADA") o un estado inventado ("INVENTADO") entraba al
 *      sistema sin resistencia. Con enum, no compila.
 *   2. Para saber que transiciones eran validas habia que leer e interpretar
 *      los anidamientos. Ahora cada estado declara sus sucesores y la regla se
 *      lee de un vistazo.
 *   3. Agregar un estado obligaba a tocar el bloque de condicionales,
 *      arriesgando los demas (Shotgun Surgery). Ahora se agrega una constante.
 *
 * Las transiciones se declaran despues del constructor porque Java no permite
 * que una constante de enum se referencie a si misma ni a otra que aun no
 * existe durante la construccion.
 */
public enum EstadoPedido {

    PENDIENTE,
    PAGADO,
    PREPARACION,
    DESPACHADO,
    ENTREGADO,
    CANCELADO;

    private Set<EstadoPedido> siguientes = Collections.emptySet();

    static {
        PENDIENTE.siguientes = EnumSet.of(PAGADO, CANCELADO);
        PAGADO.siguientes = EnumSet.of(PREPARACION, CANCELADO);
        PREPARACION.siguientes = EnumSet.of(DESPACHADO);
        DESPACHADO.siguientes = EnumSet.of(ENTREGADO);
        // ENTREGADO y CANCELADO son terminales: no tienen sucesores.
    }

    public boolean puedeTransicionarA(EstadoPedido destino) {
        return siguientes.contains(destino);
    }

    public Set<EstadoPedido> siguientesPosibles() {
        return Collections.unmodifiableSet(siguientes);
    }

    /** Un pedido cancelado devuelve la mercaderia al stock. */
    public boolean reponeStock() {
        return this == CANCELADO;
    }

    /**
     * Un pedido cuenta como facturado desde que se cobra.
     *
     * REGLA DE NEGOCIO: antes el criterio era "todo lo que no esta CANCELADO",
     * lo que metia en la misma bolsa un pedido recien ingresado y uno cobrado.
     * Un PENDIENTE es expectativa de venta, no facturacion. Esta regla se
     * asumio en el trabajo y en un caso real habria que confirmarla con el area
     * contable antes de cambiarla.
     */
    public boolean cuentaComoFacturado() {
        return this == PAGADO || this == PREPARACION
                || this == DESPACHADO || this == ENTREGADO;
    }

    public boolean esTerminal() {
        return siguientes.isEmpty();
    }

    /**
     * Interpreta el texto de un estado. Tolera minusculas y espacios porque los
     * datos entran desde interfaces donde el usuario escribe.
     *
     * @return el estado, o null si el texto no corresponde a ninguno.
     */
    public static EstadoPedido desdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(e -> e.name().equals(limpio))
                .findFirst()
                .orElse(null);
    }
}
