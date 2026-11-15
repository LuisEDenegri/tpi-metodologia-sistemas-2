package pedidos;

/**
 * Pedido de compra.
 *
 * REFACTORING: Encapsulate Field + Move Method (Fowler).
 *
 * Antes tenia trece campos publicos y cero metodos. Era el ejemplo de manual de
 * una Data Class: cualquiera podia escribir p.estado = "INVENTADO" o
 * p.total = 0 sin que nada lo impidiera, y la sonda de diagnostico demostro que
 * el sistema aceptaba ambas cosas sin resistencia.
 *
 * Ahora el pedido es responsable de su propio estado. La transicion se le pide
 * al pedido, que consulta a EstadoPedido si es valida; no hay forma de dejarlo
 * en un estado que el ciclo de vida no contemple.
 *
 * Los importes se calculan al construirlo y no cambian despues: un pedido ya
 * registrado no deberia cambiar de precio solo.
 */
public class Pedido {

    private final String numero;
    private final String documentoCliente;
    private final String codigoProducto;
    private final int cantidad;
    private final TipoEnvio tipoEnvio;
    private final Direccion direccionDeEntrega;
    private final String observaciones;
    private final boolean urgente;
    private final Dinero subtotal;
    private final Dinero envio;
    private final Dinero total;

    private EstadoPedido estado;
    private String responsable;
    private String area;

    public Pedido(String numero, SolicitudDePedido solicitud,
                  Dinero subtotal, Dinero envio) {
        this.numero = numero;
        this.documentoCliente = solicitud.documentoCliente();
        this.codigoProducto = solicitud.codigoProducto();
        this.cantidad = solicitud.cantidad();
        this.tipoEnvio = solicitud.tipoEnvio();
        this.direccionDeEntrega = solicitud.direccion();
        this.observaciones = solicitud.observaciones();
        this.urgente = solicitud.esUrgente();
        this.subtotal = subtotal;
        this.envio = envio;
        this.total = subtotal.mas(envio);
        this.estado = EstadoPedido.PENDIENTE;
    }

    /**
     * REFACTORING: Move Method. La validacion de transiciones estaba en
     * PedidoManager, en una cadena de if/else de 45 lineas. El pedido es quien
     * conoce su estado, asi que es quien debe decidir si puede cambiarlo.
     *
     * VISIBILIDAD DE PAQUETE, y no publica, a proposito.
     *
     * Un cambio de estado no es solo escribir un campo: segun el estado hay que
     * reponer stock y avisarle al cliente. Esa coordinacion vive en
     * PedidoManager.cambiarEstado. Mientras este metodo fue publico, cualquiera
     * que tuviera el Pedido (que getPedido() y pedidos() entregan) podia
     * cancelarlo por afuera y dejar el sistema incoherente: el pedido quedaba
     * CANCELADO y el inventario NO reponia las unidades.
     *
     * Era el mismo agujero que el `p.estado = "INVENTADO"` del sistema base,
     * por otra puerta: ya no se escribia un campo publico, se llamaba un metodo
     * publico que saltaba la coordinacion. Restringir la visibilidad cierra la
     * puerta sin tocar el diseno: el manager esta en este paquete y sigue
     * pudiendo; el codigo de afuera tiene que pasar por el manager.
     *
     * @return true si la transicion se aplico.
     */
    boolean cambiarEstadoA(EstadoPedido nuevoEstado) {
        if (nuevoEstado == null || !estado.puedeTransicionarA(nuevoEstado)) {
            return false;
        }
        this.estado = nuevoEstado;
        return true;
    }

    /** Visibilidad de paquete por el mismo motivo que cambiarEstadoA. */
    void asignarResponsable(String responsable, String area) {
        this.responsable = responsable;
        this.area = area;
    }

    /**
     * Direccion con la que se cotizo este pedido.
     *
     * CORRECCION: antes el pedido no guardaba su destino y habia que ir a
     * buscarlo al Cliente, que es mutable y refleja siempre el ultimo pedido.
     * Con eso, un cliente que pedia a Mendoza y despues a Cordoba dejaba el
     * primer pedido facturado con flete de Mendoza pero listado como de
     * Cordoba: el sistema cobraba un destino y mostraba otro.
     *
     * Es el mismo defecto de los datos congelados, con el signo invertido.
     * Antes el cliente nunca se actualizaba y el pedido no guardaba direccion;
     * al corregir lo primero sin corregir lo segundo, el defecto reaparecio por
     * la otra punta. La solucion de fondo es esta: un pedido historico es
     * inmutable y su destino forma parte de el.
     */
    public Direccion direccionDeEntrega() {
        return direccionDeEntrega;
    }

    /** Provincia a la que se cotizo este pedido. */
    public String provinciaDeEntrega() {
        return direccionDeEntrega == null ? null : direccionDeEntrega.provincia();
    }

    public boolean cuentaComoFacturado() {
        return estado.cuentaComoFacturado();
    }

    public boolean esDelProducto(String codigo) {
        return codigoProducto.equals(codigo);
    }

    public String numero() {
        return numero;
    }

    public String documentoCliente() {
        return documentoCliente;
    }

    public String codigoProducto() {
        return codigoProducto;
    }

    public int cantidad() {
        return cantidad;
    }

    public TipoEnvio tipoEnvio() {
        return tipoEnvio;
    }

    public String observaciones() {
        return observaciones;
    }

    public boolean esUrgente() {
        return urgente;
    }

    public Dinero subtotal() {
        return subtotal;
    }

    public Dinero envio() {
        return envio;
    }

    public Dinero total() {
        return total;
    }

    public EstadoPedido estado() {
        return estado;
    }

    public String responsable() {
        return responsable;
    }

    public String area() {
        return area;
    }

    @Override
    public String toString() {
        return numero + " | " + codigoProducto + " x" + cantidad
                + " | " + estado + " | $" + total;
    }
}
