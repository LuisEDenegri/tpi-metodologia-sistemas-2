package pedidos;

/**
 * Zona tarifaria de envio.
 *
 * REFACTORING: Replace Conditional with Polymorphism + Extract Class.
 *
 * Antes la tarifa salia de una cadena de if/else sobre el nombre de la
 * provincia, embebida en registrarPedido junto con el calculo del precio, el
 * descuento de stock y las notificaciones. Las tarifas eran numeros magicos
 * sueltos en el medio de esa logica.
 *
 * Ahora cada zona conoce su tarifa y la correspondencia provincia -> zona esta
 * en un solo lugar. Cambiar una tarifa es editar una constante; agregar una
 * zona es agregar una constante.
 */
public enum ZonaEnvio {

    MENDOZA(Dinero.de(1200)),
    AMBA(Dinero.de(2800)),
    INTERIOR(Dinero.de(4500));

    private final Dinero tarifa;

    ZonaEnvio(Dinero tarifa) {
        this.tarifa = tarifa;
    }

    public Dinero tarifa() {
        return tarifa;
    }

    /**
     * Zona que corresponde a una provincia. Una provincia desconocida se trata
     * como interior, que es la tarifa mas alta: ante la duda, no se regala el
     * flete. Es el mismo criterio que tenia el codigo original.
     */
    public static ZonaEnvio deProvincia(String provincia) {
        if (provincia == null) {
            return INTERIOR;
        }
        String p = provincia.trim();
        if (p.equalsIgnoreCase("Mendoza")) {
            return MENDOZA;
        }
        if (p.equalsIgnoreCase("CABA") || p.equalsIgnoreCase("Buenos Aires")) {
            return AMBA;
        }
        return INTERIOR;
    }
}
