package pedidos;

/**
 * Domicilio de entrega.
 *
 * REFACTORING: Introduce Parameter Object (Fowler).
 *
 * Antes calle, ciudad, provincia y cp viajaban como cuatro Strings sueltos en
 * la firma de registrarPedido. Cuatro parametros consecutivos del mismo tipo
 * son una invitacion a invertirlos: el compilador no puede distinguir
 * (calle, ciudad) de (ciudad, calle).
 *
 * Agruparlos no solo acorta la firma. Le da un lugar natural a la logica que
 * depende del domicilio: la zona de envio se calcula aca, no en el gestor de
 * pedidos.
 *
 * Inmutable: una direccion no cambia, se reemplaza por otra.
 */
public final class Direccion {

    private final String calle;
    private final String ciudad;
    private final String provincia;
    private final String codigoPostal;

    public Direccion(String calle, String ciudad, String provincia, String codigoPostal) {
        this.calle = calle;
        this.ciudad = ciudad;
        this.provincia = provincia;
        this.codigoPostal = codigoPostal;
    }

    public String calle() {
        return calle;
    }

    public String ciudad() {
        return ciudad;
    }

    public String provincia() {
        return provincia;
    }

    public String codigoPostal() {
        return codigoPostal;
    }

    /**
     * REFACTORING: Move Method. El calculo de la zona estaba dentro de
     * registrarPedido, que preguntaba por la provincia para decidir la tarifa.
     * Eso era Feature Envy: un metodo mas interesado en los datos de otro
     * objeto que en los propios. La pregunta ahora la responde quien tiene el
     * dato.
     */
    public ZonaEnvio zona() {
        return ZonaEnvio.deProvincia(provincia);
    }

    @Override
    public String toString() {
        return calle + ", " + ciudad + ", " + provincia + " (" + codigoPostal + ")";
    }
}
