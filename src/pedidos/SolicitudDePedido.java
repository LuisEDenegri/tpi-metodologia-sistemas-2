package pedidos;

/**
 * Datos de entrada para registrar un pedido.
 *
 * REFACTORING: Introduce Parameter Object (Fowler).
 *
 * Antes registrarPedido recibia 13 parametros, nueve de ellos String
 * consecutivos:
 *
 *     registrarPedido(doc, nombre, email, telefono, calle, ciudad, provincia,
 *                     cp, codigoProducto, cantidad, tipoEnvio, observaciones,
 *                     urgente)
 *
 * Una firma asi tiene tres problemas concretos:
 *
 *   1. Invertir dos argumentos del mismo tipo compila sin una sola advertencia.
 *      Confundir ciudad con provincia cambia la tarifa de envio y nadie se
 *      entera hasta que llega el reclamo.
 *   2. Agregar un dato obliga a tocar todos los llamadores (Shotgun Surgery).
 *   3. En el punto de llamada son trece valores sueltos sin nombre. Hay que ir
 *      a buscar la firma para saber que es cada uno.
 *
 * Agrupados, los datos viajan juntos porque significan algo juntos, y el objeto
 * se vuelve el lugar natural para validarlos.
 */
public final class SolicitudDePedido {

    private final String documentoCliente;
    private final String nombreCliente;
    private final String email;
    private final String telefono;
    private final Direccion direccion;
    private final String codigoProducto;
    private final int cantidad;
    private final TipoEnvio tipoEnvio;
    private final String observaciones;
    private final boolean urgente;

    public SolicitudDePedido(String documentoCliente, String nombreCliente, String email,
                             String telefono, Direccion direccion, String codigoProducto,
                             int cantidad, TipoEnvio tipoEnvio, String observaciones,
                             boolean urgente) {
        this.documentoCliente = documentoCliente;
        this.nombreCliente = nombreCliente;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.codigoProducto = codigoProducto;
        this.cantidad = cantidad;
        this.tipoEnvio = tipoEnvio;
        this.observaciones = observaciones;
        this.urgente = urgente;
    }

    /**
     * Valida la solicitud antes de que toque el sistema.
     *
     * CORRECCION: la cantidad ahora debe ser positiva. El codigo original solo
     * comprobaba "disponible < cantidad", que con una cantidad negativa pasa
     * trivialmente (10 < -5 es falso). El resultado era un pedido con subtotal
     * negativo que RESTABA de la facturacion, y un descuento de stock que al
     * restar un negativo INFLABA el inventario. Un pedido de -5 unidades subia
     * el stock de 10 a 15.
     *
     * @return el motivo del rechazo, o null si la solicitud es valida.
     */
    public String motivoDeRechazo() {
        if (esVacio(documentoCliente)) {
            return "El documento del cliente es obligatorio";
        }
        if (esVacio(codigoProducto)) {
            return "El codigo de producto es obligatorio";
        }
        if (cantidad <= 0) {
            return "La cantidad debe ser mayor a cero, se recibio " + cantidad;
        }
        if (tipoEnvio == null) {
            return "El tipo de envio no es valido";
        }
        if (direccion == null) {
            return "La direccion es obligatoria";
        }
        return null;
    }

    public boolean esValida() {
        return motivoDeRechazo() == null;
    }

    private static boolean esVacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Datos del cliente tal como vinieron en esta solicitud. */
    public Cliente comoCliente() {
        return new Cliente(documentoCliente, nombreCliente, email, telefono, direccion);
    }

    public String documentoCliente() {
        return documentoCliente;
    }

    public String nombreCliente() {
        return nombreCliente;
    }

    public String email() {
        return email;
    }

    public String telefono() {
        return telefono;
    }

    public Direccion direccion() {
        return direccion;
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
}
