package pedidos;

/**
 * Reglas comerciales de precios y envio.
 *
 * REFACTORING: Extract Class (Fowler).
 *
 * Antes estas reglas vivian dentro de registrarPedido, mezcladas con la
 * creacion del pedido, el descuento de stock y el envio de notificaciones.
 * Esa mezcla es la razon por la que el defecto principal del sistema sobrevivio
 * tanto tiempo sin que nadie lo viera: estaba en la linea 100 de un metodo de
 * 87 lineas que hacia otras seis cosas.
 *
 * Separadas, las reglas se leen, se discuten con el area comercial y se
 * prueban sin levantar el resto del sistema.
 */
public class PoliticaDeCostos {

    /**
     * Monto a partir del cual se bonifica el flete estandar.
     *
     * CORRECCION: la comparacion ahora es "mayor o igual". Antes era "mayor"
     * estricta, asi que un pedido de exactamente $25.000 pagaba flete completo.
     * Si la promocion publicada dice "envio gratis desde $25.000", esa
     * diferencia de un centavo incumple la oferta y expone a un reclamo de
     * Defensa del Consumidor. Leyendo solo el codigo no habia forma de saber si
     * el ">" era deliberado o un error: por eso la regla se documenta aca.
     */
    public static final Dinero MINIMO_PARA_ENVIO_BONIFICADO = Dinero.de(25000);

    /** Recargo por logistica prioritaria, como porcentaje de la tarifa de zona. */
    public static final int RECARGO_URGENTE_PORCENTAJE = 50;

    /**
     * Precio unitario segun el codigo de producto.
     *
     * ponytail: techo deliberado. Los precios estan en el codigo, igual que en
     * el sistema original, porque el alcance del trabajo es mejorar la calidad
     * interna sin agregar funcionalidad. En produccion esto sale de una tabla
     * de catalogo: la lista de precios cambia por inflacion y no puede requerir
     * un deploy. El refactoring deja el reemplazo acotado a este metodo.
     */
    public Dinero precioUnitario(String codigoProducto) {
        if (codigoProducto == null) {
            return Dinero.de(5000);
        }
        if (codigoProducto.startsWith("VIN")) {
            return Dinero.de(8500);
        }
        if (codigoProducto.startsWith("ACE")) {
            return Dinero.de(6200);
        }
        if (codigoProducto.startsWith("DUL")) {
            return Dinero.de(3400);
        }
        if (codigoProducto.startsWith("FRU")) {
            return Dinero.de(4800);
        }
        return Dinero.de(5000);
    }

    public Dinero subtotal(String codigoProducto, int cantidad) {
        return precioUnitario(codigoProducto).por(cantidad);
    }

    /**
     * Costo de envio.
     *
     * CORRECCION DEL DEFECTO PRINCIPAL. El codigo original hacia:
     *
     *     if (urgente)            envio = envio * 1.5;
     *     if (subtotal > 25000)   envio = 0;       // <-- borra lo anterior
     *
     * El segundo if pisaba al primero, asi que el recargo por urgencia
     * desaparecia justo en los pedidos mas grandes: el servicio premium se
     * regalaba a quien mas compraba. Un pedido urgente de $34.000 salia
     * exactamente lo mismo que uno normal.
     *
     * REGLA CORREGIDA: la bonificacion por compra grande cubre el FLETE
     * ESTANDAR. El servicio urgente es un adicional por logistica prioritaria y
     * se cobra siempre.
     *
     *     envio = (bonificado ? 0 : tarifa) + (urgente ? tarifa * 50% : 0)
     *
     * Esta formula deja intacto el caso que ya funcionaba bien (bajo el umbral,
     * un urgente paga tarifa * 1.5) y solo corrige el que estaba roto.
     *
     * Es un cambio de comportamiento, no un refactoring: altera lo que el
     * sistema cobra. Por eso esta separado de los refactorings en el historial
     * y requiere confirmacion comercial antes de ir a produccion.
     */
    public Dinero costoDeEnvio(TipoEnvio tipoEnvio, ZonaEnvio zona,
                               Dinero subtotal, boolean urgente) {
        if (tipoEnvio == TipoEnvio.RETIRO) {
            return Dinero.CERO;
        }

        Dinero tarifa = zona.tarifa();
        Dinero flete = tieneEnvioBonificado(subtotal) ? Dinero.CERO : tarifa;

        if (urgente) {
            flete = flete.mas(tarifa.porcentaje(RECARGO_URGENTE_PORCENTAJE));
        }
        return flete;
    }

    public boolean tieneEnvioBonificado(Dinero subtotal) {
        return subtotal.esMayorOIgualA(MINIMO_PARA_ENVIO_BONIFICADO);
    }
}
