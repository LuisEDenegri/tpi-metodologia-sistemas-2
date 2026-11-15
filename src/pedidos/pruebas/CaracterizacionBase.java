package pedidos.pruebas;

import java.util.List;
import pedidos.Pedido;
import pedidos.PedidoManager;

/**
 * PRUEBAS DE CARACTERIZACION del sistema base.
 *
 * Estas pruebas NO dicen como deberia comportarse el sistema. Dicen como se
 * comporta HOY, defectos incluidos. Ese es exactamente su proposito: son la red
 * de seguridad del refactoring.
 *
 * La distincion importa y es el centro del trabajo:
 *
 *   - REFACTORING = cambiar la estructura SIN cambiar el comportamiento.
 *     Estas pruebas tienen que seguir pasando despues de refactorizar. Si una
 *     falla, el refactoring rompio algo.
 *
 *   - CORRECCION DE DEFECTO = cambiar el comportamiento a proposito.
 *     Eso es otra cosa, y se verifica con las pruebas de ComportamientoCorrecto.
 *
 * Por eso aca hay casos que fijan comportamiento que SABEMOS que esta mal, con
 * el defecto anotado al lado. Mezclar ambas cosas haria imposible distinguir
 * "rompi algo sin querer" de "corregi algo a proposito".
 *
 * Michael Feathers, "Working Effectively with Legacy Code": una prueba de
 * caracterizacion documenta la verdad, no el deseo.
 */
public class CaracterizacionBase {

    public static void main(String[] args) {
        Verificador v = new Verificador("CARACTERIZACION DEL SISTEMA BASE");

        calculoDeImportes(v);
        cicloDeVidaDelPedido(v);
        inventario(v);
        busqueda(v);
        reporte(v);
        comportamientoDefectuosoCongelado(v);

        boolean ok = v.resumen();
        salir(args, ok);
    }

    /**
     * Sale con codigo de error salvo que la invoque CorridaDePruebas, que
     * necesita seguir viva para ejecutar la segunda suite.
     */
    static void salir(String[] args, boolean ok) {
        boolean anidada = args.length > 0 && args[0].equals("--sin-salir");
        if (!anidada) {
            System.exit(ok ? 0 : 1);
        }
    }

    static PedidoManager conStock() {
        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 100);
        m.cargarStock("ACE-OLIVA-500", 100);
        m.cargarStock("DUL-MEMBRILLO-400", 100);
        m.cargarStock("FRU-NUEZ-250", 100);
        m.cargarStock("OTR-GENERICO-1", 100);
        return m;
    }

    static String pedir(PedidoManager m, String doc, String provincia, String codigo,
                        int cantidad, String tipoEnvio, boolean urgente) {
        return m.registrarPedido(doc, "Cliente " + doc, doc + "@ejemplo.com", "261000",
                "Calle 1", "Ciudad", provincia, "5500", codigo, cantidad,
                tipoEnvio, "", urgente);
    }

    // ------------------------------------------------------------------
    static void calculoDeImportes(Verificador v) {
        PedidoManager m = conStock();

        String vino = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        v.igual("precio del vino por unidad", 8500.0, m.getPedido(vino).subtotal);

        String aceite = pedir(m, "2", "Mendoza", "ACE-OLIVA-500", 1, "RETIRO", false);
        v.igual("precio del aceite", 6200.0, m.getPedido(aceite).subtotal);

        String dulce = pedir(m, "3", "Mendoza", "DUL-MEMBRILLO-400", 1, "RETIRO", false);
        v.igual("precio del dulce", 3400.0, m.getPedido(dulce).subtotal);

        String fruto = pedir(m, "4", "Mendoza", "FRU-NUEZ-250", 1, "RETIRO", false);
        v.igual("precio de frutos secos", 4800.0, m.getPedido(fruto).subtotal);

        String otro = pedir(m, "5", "Mendoza", "OTR-GENERICO-1", 1, "RETIRO", false);
        v.igual("precio por defecto de un codigo desconocido", 5000.0, m.getPedido(otro).subtotal);

        String dos = pedir(m, "6", "Mendoza", "VIN-MALBEC-750", 2, "RETIRO", false);
        v.igual("el subtotal multiplica por la cantidad", 17000.0, m.getPedido(dos).subtotal);

        // fletes por zona, sin alcanzar el umbral de envio gratis
        String mza = pedir(m, "7", "Mendoza", "DUL-MEMBRILLO-400", 1, "ENVIO", false);
        v.igual("flete a Mendoza", 1200.0, m.getPedido(mza).envio);

        String caba = pedir(m, "8", "CABA", "DUL-MEMBRILLO-400", 1, "ENVIO", false);
        v.igual("flete a CABA", 2800.0, m.getPedido(caba).envio);

        String bsas = pedir(m, "9", "Buenos Aires", "DUL-MEMBRILLO-400", 1, "ENVIO", false);
        v.igual("flete a Buenos Aires", 2800.0, m.getPedido(bsas).envio);

        String interior = pedir(m, "10", "Cordoba", "DUL-MEMBRILLO-400", 1, "ENVIO", false);
        v.igual("flete al interior", 4500.0, m.getPedido(interior).envio);

        String retiro = pedir(m, "11", "Cordoba", "DUL-MEMBRILLO-400", 1, "RETIRO", false);
        v.igual("el retiro en local no paga flete", 0.0, m.getPedido(retiro).envio);

        String urg = pedir(m, "12", "Mendoza", "DUL-MEMBRILLO-400", 1, "ENVIO", true);
        v.igual("el urgente recarga el flete un 50%", 1800.0, m.getPedido(urg).envio);

        String gratis = pedir(m, "13", "Cordoba", "VIN-MALBEC-750", 4, "ENVIO", false);
        v.igual("sobre 25000 el flete es gratis", 0.0, m.getPedido(gratis).envio);

        String total = pedir(m, "14", "Mendoza", "DUL-MEMBRILLO-400", 2, "ENVIO", false);
        v.igual("el total suma subtotal mas flete", 8000.0, m.getPedido(total).total);
    }

    // ------------------------------------------------------------------
    static void cicloDeVidaDelPedido(Verificador v) {
        PedidoManager m = conStock();

        String p = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        v.igual("un pedido nuevo nace PENDIENTE", "PENDIENTE", m.getPedido(p).estado);

        v.cierto("PENDIENTE avanza a PAGADO", m.cambiarEstado(p, "PAGADO"));
        v.cierto("PAGADO avanza a PREPARACION", m.cambiarEstado(p, "PREPARACION"));
        v.cierto("PREPARACION avanza a DESPACHADO", m.cambiarEstado(p, "DESPACHADO"));
        v.cierto("DESPACHADO avanza a ENTREGADO", m.cambiarEstado(p, "ENTREGADO"));
        v.igual("el pedido termina ENTREGADO", "ENTREGADO", m.getPedido(p).estado);

        String q = pedir(m, "2", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        v.falso("no se puede saltear de PENDIENTE a ENTREGADO",
                m.cambiarEstado(q, "ENTREGADO"));
        v.igual("el pedido no cambio de estado tras la transicion invalida",
                "PENDIENTE", m.getPedido(q).estado);

        v.falso("no se acepta un estado inventado", m.cambiarEstado(q, "INVENTADO"));
        v.falso("no se puede cambiar el estado de un pedido inexistente",
                m.cambiarEstado("PED-9999", "PAGADO"));

        String c = pedir(m, "3", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        v.cierto("PENDIENTE se puede cancelar", m.cambiarEstado(c, "CANCELADO"));
        v.falso("CANCELADO es terminal: no se recancela",
                m.cambiarEstado(c, "CANCELADO"));
        v.falso("CANCELADO es terminal: no revive a PAGADO",
                m.cambiarEstado(c, "PAGADO"));

        String e = pedir(m, "4", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        m.cambiarEstado(e, "PAGADO");
        v.cierto("PAGADO tambien se puede cancelar", m.cambiarEstado(e, "CANCELADO"));

        String d = pedir(m, "5", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        m.cambiarEstado(d, "PAGADO");
        m.cambiarEstado(d, "PREPARACION");
        v.falso("desde PREPARACION ya no se puede cancelar",
                m.cambiarEstado(d, "CANCELADO"));

        v.cierto("se asigna responsable", m.asignarResponsable(d, "Laura Diaz", "DEPOSITO"));
        v.igual("queda guardado el responsable", "Laura Diaz", m.getPedido(d).responsable);
        v.igual("queda guardada el area", "DEPOSITO", m.getPedido(d).area);
        v.falso("no se asigna a un pedido inexistente",
                m.asignarResponsable("PED-9999", "X", "Y"));
    }

    // ------------------------------------------------------------------
    static void inventario(Verificador v) {
        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 10);

        v.igual("el stock cargado se consulta", 10, m.stockDe("VIN-MALBEC-750"));
        v.igual("un producto no cargado tiene stock cero", 0, m.stockDe("NO-EXISTE"));

        String p = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 3, "RETIRO", false);
        v.igual("el pedido descuenta del stock", 7, m.stockDe("VIN-MALBEC-750"));

        String rechazado = pedir(m, "2", "Mendoza", "VIN-MALBEC-750", 999, "RETIRO", false);
        v.igual("sin stock suficiente devuelve null", null, rechazado);
        v.igual("el stock no se toca si el pedido se rechaza", 7, m.stockDe("VIN-MALBEC-750"));

        m.cambiarEstado(p, "CANCELADO");
        v.igual("cancelar repone el stock", 10, m.stockDe("VIN-MALBEC-750"));

        String exacto = pedir(m, "3", "Mendoza", "VIN-MALBEC-750", 10, "RETIRO", false);
        v.cierto("se puede pedir exactamente todo el stock", exacto != null);
        v.igual("el stock queda en cero", 0, m.stockDe("VIN-MALBEC-750"));
    }

    // ------------------------------------------------------------------
    static void busqueda(Verificador v) {
        PedidoManager m = conStock();

        String a = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        String b = pedir(m, "2", "Cordoba", "ACE-OLIVA-500", 1, "ENVIO", true);
        pedir(m, "3", "CABA", "DUL-MEMBRILLO-400", 1, "ENVIO", false);
        m.cambiarEstado(a, "PAGADO");

        v.igual("sin filtros devuelve todo", 3, m.buscar(null, null, null, null, null).size());
        v.igual("filtra por estado", 1, m.buscar("PAGADO", null, null, null, null).size());
        v.igual("filtra por provincia", 1, m.buscar(null, "Cordoba", null, null, null).size());
        v.igual("filtra por urgencia", 1, m.buscar(null, null, null, true, null).size());
        v.igual("filtra por producto", 1,
                m.buscar(null, null, null, null, "VIN-MALBEC-750").size());

        List<Pedido> caros = m.buscar(null, null, 5000.0, null, null);
        v.cierto("filtra por monto minimo", caros.size() >= 1);
        for (Pedido p : caros) {
            v.cierto("todo resultado supera el monto pedido", p.total >= 5000.0);
        }

        v.igual("combinar filtros los aplica juntos", 1,
                m.buscar(null, "Cordoba", null, true, null).size());
        v.igual("filtros sin coincidencia devuelven vacio", 0,
                m.buscar("ENTREGADO", null, null, null, null).size());
        v.cierto("el pedido urgente es el de Cordoba",
                m.buscar(null, null, null, true, null).get(0).numero.equals(b));
    }

    // ------------------------------------------------------------------
    static void reporte(Verificador v) {
        PedidoManager m = conStock();

        String a = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        String b = pedir(m, "2", "Mendoza", "ACE-OLIVA-500", 1, "RETIRO", false);
        m.cambiarEstado(a, "PAGADO");
        m.cambiarEstado(b, "CANCELADO");

        String rep = m.reporte();

        v.cierto("el reporte tiene encabezado", rep.contains("=== REPORTE DE PEDIDOS ==="));
        v.cierto("lista cada pedido", rep.contains(a) && rep.contains(b));
        v.cierto("cuenta los pagados", rep.contains("--- Pagados: 1"));
        v.cierto("cuenta los cancelados", rep.contains("--- Cancelados: 1"));
        v.cierto("informa el facturado", rep.contains("--- Facturado:"));

        v.igual("el cancelado no suma al facturado", 8500.0, m.totalFacturado());

        String urg = pedir(m, "3", "Mendoza", "VIN-MALBEC-750", 1, "ENVIO", true);
        v.cierto("marca los urgentes", m.reporte().contains("URGENTE"));
        v.cierto("el pedido urgente existe", urg != null);
    }

    // ------------------------------------------------------------------
    /**
     * Comportamiento que sabemos INCORRECTO, congelado a proposito.
     *
     * Estos casos documentan los defectos vigentes. Siguen el principio de
     * caracterizacion: fijan lo que el sistema hace hoy para que el refactoring
     * no lo altere por accidente. Cuando los defectos se corrijan, estos casos
     * van a fallar, y esa falla sera la senal de que el arreglo surtio efecto.
     *
     * Cada uno remite al defecto que lo origina.
     */
    static void comportamientoDefectuosoCongelado(Verificador v) {
        PedidoManager m = conStock();

        // DEFECTO 1: el recargo por urgente se pierde sobre el umbral.
        String urgenteGrande = pedir(m, "1", "Cordoba", "VIN-MALBEC-750", 4, "ENVIO", true);
        String normalGrande = pedir(m, "2", "Cordoba", "VIN-MALBEC-750", 4, "ENVIO", false);
        v.igual("[defecto 1] el urgente grande no paga recargo",
                m.getPedido(normalGrande).total, m.getPedido(urgenteGrande).total);

        // DEFECTO del umbral: la comparacion es estricta.
        String justo = pedir(m, "3", "Mendoza", "OTR-GENERICO-1", 5, "ENVIO", false);
        v.igual("[defecto] subtotal de exactamente 25000 paga flete",
                1200.0, m.getPedido(justo).envio);

        // DEFECTO: cantidad cero se acepta.
        String cero = pedir(m, "4", "Mendoza", "VIN-MALBEC-750", 0, "ENVIO", false);
        v.cierto("[defecto] se acepta un pedido de cantidad cero", cero != null);

        // DEFECTO: tipoEnvio sensible a mayusculas.
        String minuscula = pedir(m, "5", "Mendoza", "VIN-MALBEC-750", 1, "retiro", false);
        v.igual("[defecto] 'retiro' en minuscula cobra flete",
                1200.0, m.getPedido(minuscula).envio);

        // DEFECTO: cargarStock pisa en vez de sumar.
        PedidoManager m2 = new PedidoManager();
        m2.cargarStock("X", 50);
        m2.cargarStock("X", 20);
        v.igual("[defecto] cargarStock reemplaza el valor", 20, m2.stockDe("X"));

        // DEFECTO: el cliente queda aunque el pedido se rechace.
        PedidoManager m3 = new PedidoManager();
        m3.cargarStock("Y", 1);
        pedir(m3, "77", "Mendoza", "Y", 999, "ENVIO", false);
        v.cierto("[defecto] el cliente queda tras un pedido rechazado",
                m3.clientes.containsKey("77"));

        // DEFECTO: 'facturado' incluye pedidos no pagados.
        PedidoManager m4 = conStock();
        pedir(m4, "1", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        v.igual("[defecto] un PENDIENTE ya cuenta como facturado",
                8500.0, m4.totalFacturado());
    }
}
