package pedidos;

/**
 * Cliente de la distribuidora, identificado por su documento.
 *
 * REFACTORING: Encapsulate Field (Fowler).
 *
 * Antes tenia ocho campos publicos y cero metodos: una Data Class, estructura
 * de datos disfrazada de objeto. Cualquier codigo podia escribirle encima sin
 * pasar por ninguna validacion.
 *
 * Los datos de contacto ahora se actualizan a traves de un metodo. Eso corrige
 * un defecto real: antes, cuando un cliente ya existente volvia a comprar con
 * datos distintos, el bloque de alta se saltaba por completo y los datos
 * quedaban congelados en el primer pedido. La consecuencia era concreta: a un
 * cliente mudado de Mendoza a Cordoba se le cobraba el flete de Cordoba
 * (porque la tarifa usaba el dato nuevo del formulario) pero el paquete se
 * despachaba al domicilio viejo de Mendoza. Se cobraba un destino y se enviaba
 * a otro.
 */
public class Cliente {

    private final String documento;
    private String nombre;
    private String email;
    private String telefono;
    private Direccion direccion;

    public Cliente(String documento, String nombre, String email, String telefono,
                   Direccion direccion) {
        this.documento = documento;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    /**
     * Actualiza los datos de contacto con los del pedido mas reciente.
     * El documento no se toca: es la identidad del cliente.
     *
     * REGLA: un pedido actualiza los datos que TRAE; no borra los que no trae.
     * Un campo vacio o null significa "este pedido no aporta ese dato", no
     * "borrar el dato que ya teniamos".
     *
     * Esta guarda cierra un defecto que introdujo el propio refactoring. Al
     * corregir el defecto de los datos congelados, la actualizacion paso a ser
     * incondicional; como SolicitudDePedido no exige nombre, email ni telefono,
     * un pedido perfectamente valido sin esos campos borraba los datos buenos
     * del cliente. Peor: al quedarse sin mail, el cliente dejaba de recibir
     * TODA notificacion futura. Se corregia un defecto creando otro.
     *
     * Se eligio esta regla y no "validar el contacto en la solicitud" porque el
     * dato obligatorio del negocio es el documento: es lo que identifica a
     * quien compra. Exigir mail rechazaria pedidos legitimos tomados por
     * telefono o en mostrador. Entre perder un dato y perder una venta, se
     * conserva el dato.
     *
     * Visibilidad de paquete: el estado del cliente lo coordina PedidoManager,
     * no el codigo externo que recibe el Cliente desde clientes() o getCliente().
     */
    void actualizarDatosDeContacto(String nombre, String email, String telefono,
                                   Direccion direccion) {
        if (!esVacio(nombre)) {
            this.nombre = nombre;
        }
        if (!esVacio(email)) {
            this.email = email;
        }
        if (!esVacio(telefono)) {
            this.telefono = telefono;
        }
        if (direccion != null) {
            this.direccion = direccion;
        }
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    public String documento() {
        return documento;
    }

    public String nombre() {
        return nombre;
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

    public String provincia() {
        return direccion == null ? null : direccion.provincia();
    }

    @Override
    public String toString() {
        return nombre + " (" + documento + ")";
    }
}
