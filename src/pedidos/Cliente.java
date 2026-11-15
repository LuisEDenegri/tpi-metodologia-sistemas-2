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
     */
    public void actualizarDatosDeContacto(String nombre, String email, String telefono,
                                          Direccion direccion) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
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
