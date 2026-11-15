package sondas;

import pedidos.PedidoManager;

/**
 * Segunda sonda: verifica los hallazgos que aporto la revision tecnica
 * independiente y que la primera sonda no habia buscado.
 *
 * Misma regla que la sonda anterior: se compara contra el valor esperado y se
 * rotula segun el resultado real. Un hallazgo de un revisor tampoco se acepta
 * sin ejecutarlo.
 */
public class SondaRevision {

    private static int confirmados = 0;
    private static int descartados = 0;

    public static void main(String[] args) {
        cantidadCeroONegativa();
        reposicionPisaRecuentoDeDeposito();
        cargarStockPisaEnVezDeSumar();
        tipoEnvioEsSensibleAMayusculas();
        bordeExactoDelUmbral();
        catchVacioOcultaFallaDeNotificacion();
        clienteQuedaAunqueElPedidoSeRechace();
        reporteEnNotacionCientifica();
        facturadoIncluyeNoPagados();

        System.out.println();
        System.out.println("=========================================================");
        System.out.println("RESUMEN REVISION: " + confirmados + " confirmados, "
                + descartados + " descartados");
        System.out.println("=========================================================");
    }

    static void titulo(String t) {
        System.out.println();
        System.out.println("=========================================================");
        System.out.println(t);
        System.out.println("=========================================================");
    }

    static void verificar(String hipotesis, boolean hayDefecto, String detalle) {
        if (hayDefecto) {
            confirmados++;
            System.out.println("  [CONFIRMADO] " + hipotesis);
        } else {
            descartados++;
            System.out.println("  [DESCARTADO] " + hipotesis);
        }
        System.out.println("               " + detalle);
    }

    /** B2: no se valida que la cantidad sea positiva. */
    static void cantidadCeroONegativa() {
        titulo("REV-1: cantidad cero o negativa se acepta");

        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 10);

        String cero = m.registrarPedido("1", "A", "a@e.com", "1", "c", "ci",
                "Mendoza", "5500", "VIN-MALBEC-750", 0, "ENVIO", "", false);
        System.out.println("  cantidad 0  -> pedido " + cero
                + ", total " + (cero != null ? m.getPedido(cero).total : "n/a"));

        int antes = m.stockDe("VIN-MALBEC-750");
        String neg = m.registrarPedido("2", "B", "b@e.com", "1", "c", "ci",
                "Mendoza", "5500", "VIN-MALBEC-750", -5, "ENVIO", "", false);
        int despues = m.stockDe("VIN-MALBEC-750");

        System.out.println("  cantidad -5 -> pedido " + neg
                + ", subtotal " + (neg != null ? m.getPedido(neg).subtotal : "n/a"));
        System.out.println("  stock antes " + antes + " -> despues " + despues);

        boolean creaPedidoVacio = cero != null;
        boolean inflaStock = despues > antes;

        verificar("cantidad <= 0 se acepta y corrompe stock y contabilidad",
                creaPedidoVacio && inflaStock,
                "La guarda 'disponible < cantidad' pasa trivialmente con negativos ("
                        + antes + " < -5 es falso). Luego el descuento suma: el stock subio de "
                        + antes + " a " + despues + ". El pedido entra con subtotal negativo, "
                        + "que RESTA del facturado.");
    }

    /** B3: la reposicion al cancelar suma sobre un valor que pudo cambiar. */
    static void reposicionPisaRecuentoDeDeposito() {
        titulo("REV-2: cancelar despues de un recuento de deposito infla stock");

        PedidoManager m = new PedidoManager();
        m.cargarStock("ACE-OLIVA-500", 30);
        String p = m.registrarPedido("1", "A", "a@e.com", "1", "c", "ci",
                "Mendoza", "5500", "ACE-OLIVA-500", 10, "RETIRO", "", false);
        System.out.println("  stock 30, pedido de 10 -> " + m.stockDe("ACE-OLIVA-500"));

        m.cargarStock("ACE-OLIVA-500", 100);
        System.out.println("  llega camion, recuento fisico = 100 -> " + m.stockDe("ACE-OLIVA-500"));

        m.cambiarEstado(p, "CANCELADO");
        int finalStock = m.stockDe("ACE-OLIVA-500");
        System.out.println("  se cancela el pedido viejo -> " + finalStock);

        verificar("la reposicion suma sobre el recuento nuevo e inventa unidades",
                finalStock != 100,
                "Quedo en " + finalStock + " cuando el deposito tiene 100 reales. La mercaderia "
                        + "del pedido cancelado nunca salio, ya estaba contada en el recuento. "
                        + "El sistema no distingue stock reservado de stock disponible.");
    }

    /** B9: cargarStock reemplaza aunque el nombre sugiere ingreso. */
    static void cargarStockPisaEnVezDeSumar() {
        titulo("REV-3: cargarStock pisa en vez de sumar");

        PedidoManager m = new PedidoManager();
        m.cargarStock("DUL-MEMBRILLO-400", 50);
        m.cargarStock("DUL-MEMBRILLO-400", 20);
        int r = m.stockDe("DUL-MEMBRILLO-400");

        System.out.println("  cargarStock(50) y despues cargarStock(20) -> " + r);

        verificar("el nombre dice cargar pero la implementacion reemplaza",
                r == 20,
                "Quedo en " + r + ". Quien recibe 20 cajas mas y llama cargarStock(20) "
                        + "acaba de borrar 50 unidades del inventario. O el metodo suma, "
                        + "o se llama fijarStock.");
    }

    /** B11: la comparacion de tipoEnvio es sensible a mayusculas. */
    static void tipoEnvioEsSensibleAMayusculas() {
        titulo("REV-4: tipoEnvio sin validar, sensible a mayusculas");

        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 100);

        String mayus = m.registrarPedido("1", "A", "a@e.com", "1", "c", "ci",
                "Mendoza", "5500", "VIN-MALBEC-750", 1, "RETIRO", "", false);
        String minus = m.registrarPedido("2", "B", "b@e.com", "1", "c", "ci",
                "Mendoza", "5500", "VIN-MALBEC-750", 1, "retiro", "", false);
        String basura = m.registrarPedido("3", "C", "c@e.com", "1", "c", "ci",
                "Mendoza", "5500", "VIN-MALBEC-750", 1, "CUALQUIERCOSA", "", false);

        double eMayus = m.getPedido(mayus).envio;
        double eMinus = m.getPedido(minus).envio;
        double eBasura = m.getPedido(basura).envio;

        System.out.println("  tipoEnvio 'RETIRO'        -> envio " + eMayus);
        System.out.println("  tipoEnvio 'retiro'        -> envio " + eMinus);
        System.out.println("  tipoEnvio 'CUALQUIERCOSA' -> envio " + eBasura);

        verificar("'retiro' en minuscula cobra flete y la basura se acepta",
                eMinus > 0 && eBasura > 0,
                "Al cliente que retira en el local escribiendo 'retiro' se le cobran "
                        + eMinus + " de flete. Y un tipo de envio inexistente entra al "
                        + "sistema sin resistencia.");
    }

    /** A7: el umbral usa > estricto. Exactamente 25000 paga flete. */
    static void bordeExactoDelUmbral() {
        titulo("REV-5: borde exacto del umbral de envio gratis");

        PedidoManager m = new PedidoManager();
        m.cargarStock("BOR-TEST-001", 100);

        // precio por defecto 5000 (no matchea ningun prefijo conocido) x5 = 25000
        String justo = m.registrarPedido("1", "A", "a@e.com", "1", "c", "ci",
                "Mendoza", "5500", "BOR-TEST-001", 5, "ENVIO", "", false);
        String unoMas = m.registrarPedido("2", "B", "b@e.com", "1", "c", "ci",
                "Mendoza", "5500", "BOR-TEST-001", 6, "ENVIO", "", false);

        double sJusto = m.getPedido(justo).subtotal;
        double eJusto = m.getPedido(justo).envio;
        double eUnoMas = m.getPedido(unoMas).envio;

        System.out.println("  subtotal exacto " + sJusto + " -> envio " + eJusto);
        System.out.println("  subtotal " + m.getPedido(unoMas).subtotal + " -> envio " + eUnoMas);

        verificar("un subtotal de exactamente 25000 paga flete completo",
                sJusto == 25000 && eJusto > 0,
                "La condicion es 'subtotal > 25000', estricta. Si la promocion publicada "
                        + "dice 'desde $25.000' esto incumple la oferta y expone a un reclamo "
                        + "de Defensa del Consumidor. Si dice 'mas de', es correcto, pero "
                        + "leyendo el codigo no hay forma de saber cual de las dos es.");
    }

    /** B4: el catch vacio se traga el NPE y devuelve true igual. */
    static void catchVacioOcultaFallaDeNotificacion() {
        titulo("REV-6: catch vacio confirma el cambio de estado sin notificar");

        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 100);
        String p = m.registrarPedido("77", "A", "a@e.com", "1", "c", "ci",
                "Mendoza", "5500", "VIN-MALBEC-750", 1, "RETIRO", "", false);

        // se da de baja el cliente: cualquier inconsistencia de datos
        m.clientes.remove("77");
        System.out.println("  se elimina el cliente del mapa y se cambia el estado:");

        boolean r = m.cambiarEstado(p, "PAGADO");
        String estado = m.getPedido(p).estado;

        System.out.println("  cambiarEstado devolvio: " + r);
        System.out.println("  estado del pedido:      " + estado);
        System.out.println("  (no se imprimio ningun MAIL ni ANALYTICS arriba)");

        verificar("devuelve true aunque la notificacion fallo sin dejar rastro",
                r && estado.equals("PAGADO"),
                "clientes.get devuelve null, c.email lanza NPE y el catch vacio se la traga. "
                        + "El llamador cree que todo salio bien. El dia que EmailService hable "
                        + "con un SMTP real, el sistema dejara de notificar a todos los clientes "
                        + "y nadie se entera hasta que llame el primero.");
    }

    /** B10: el cliente se persiste antes de validar el stock. */
    static void clienteQuedaAunqueElPedidoSeRechace() {
        titulo("REV-7: el cliente queda registrado aunque el pedido se rechace");

        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 2);

        String r = m.registrarPedido("99999", "Carlos Ruiz", "carlos@e.com", "1", "c", "ci",
                "Mendoza", "5500", "VIN-MALBEC-750", 999, "ENVIO", "", false);

        boolean quedoCliente = m.clientes.containsKey("99999");
        System.out.println("  pedido sin stock devolvio: " + r);
        System.out.println("  el cliente quedo en la base: " + quedoCliente);

        verificar("registrarPedido no es atomico: falla y deja residuo",
                r == null && quedoCliente,
                "El cliente se persiste ANTES de validar el stock. Un pedido que nunca "
                        + "existio deja un cliente dado de alta. En el Main real, 'Carlos Ruiz' "
                        + "queda en la base aunque su pedido fue rechazado.");
    }

    /** B5: el reporte imprime notacion cientifica sobre 10^7. */
    static void reporteEnNotacionCientifica() {
        titulo("REV-8: el reporte imprime notacion cientifica");

        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 5000);
        m.registrarPedido("1", "G", "g@e.com", "1", "c", "ci",
                "Mendoza", "5500", "VIN-MALBEC-750", 2000, "RETIRO", "", false);

        String rep = m.reporte();
        String lineaFact = "";
        for (String l : rep.split("\n")) {
            if (l.contains("Facturado")) {
                lineaFact = l;
            }
        }
        System.out.println("  " + lineaFact);

        verificar("el reporte contable sale en notacion cientifica",
                lineaFact.contains("E7") || lineaFact.contains("E8"),
                "La concatenacion '$' + double delega el formato a Double.toString(), que "
                        + "pasa a notacion cientifica desde 10^7. Un reporte que va a "
                        + "administracion diciendo $1.7E7 es ilegible. Esperado: $17.000.000,00");
    }

    /** B13: "Facturado" suma pedidos que nadie pago. */
    static void facturadoIncluyeNoPagados() {
        titulo("REV-9: 'Facturado' incluye pedidos que nadie pago");

        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 100);
        String p = m.registrarPedido("1", "A", "a@e.com", "1", "c", "ci",
                "Mendoza", "5500", "VIN-MALBEC-750", 1, "RETIRO", "", false);

        String estado = m.getPedido(p).estado;
        double fact = m.totalFacturado();

        System.out.println("  unico pedido en estado: " + estado);
        System.out.println("  total facturado:        " + fact);

        verificar("un pedido recien ingresado ya cuenta como facturado",
                estado.equals("PENDIENTE") && fact > 0,
                "El criterio es 'todo lo que no esta CANCELADO', lo que mete en la misma "
                        + "bolsa un pedido recien ingresado y uno entregado y cobrado. Si el "
                        + "reporte se usa para proyeccion de ingresos, el numero esta inflado. "
                        + "Requiere confirmacion del area contable: puede ser intencional.");
    }
}
