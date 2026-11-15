package sondas;

import java.util.List;
import pedidos.Cliente;
import pedidos.Dinero;
import pedidos.Direccion;
import pedidos.EstadoPedido;
import pedidos.FiltroDePedidos;
import pedidos.Inventario;
import pedidos.NotificadorPorEmail;
import pedidos.NotificadorSilencioso;
import pedidos.Pedido;
import pedidos.PedidoManager;
import pedidos.PoliticaDeCostos;
import pedidos.SolicitudDePedido;
import pedidos.TipoEnvio;

/**
 * Tercera sonda: re-ejecuta el diagnostico completo contra el sistema YA
 * REFACTORIZADO.
 *
 * Las dos sondas anteriores (SondaDeDefectos y SondaRevision) demostraron por
 * ejecucion que existian 15 defectos. Fueron escritas contra la API vieja y
 * quedaron como evidencia historica del estado inicial, congeladas junto al tag
 * estado-inicial.
 *
 * Esta sonda hace la pregunta inversa: de aquellos 15 defectos demostrados,
 * cuantos siguen presentes. Mantiene el mismo metodo de trabajo que las
 * anteriores y por la misma razon: cada veredicto se calcula comparando el
 * valor real contra el esperado. Un defecto no se declara corregido porque el
 * refactoring "deberia" haberlo arreglado, sino porque se ejecuta el escenario
 * que lo exhibia y el sistema ya no se comporta asi.
 *
 * Incluye tambien el caso que la primera sonda DESCARTO (la doble cancelacion),
 * para verificar que el refactoring no lo convirtio en un defecto real.
 */
public class SondaDeRegresion {

    private static int corregidos = 0;
    private static int pendientes = 0;
    private static int atenuados = 0;
    private static int descartados = 0;

    public static void main(String[] args) {
        System.out.println();
        System.out.println("#########################################################");
        System.out.println("#  VERIFICACION DE REGRESION SOBRE EL SISTEMA NUEVO     #");
        System.out.println("#  Los 15 defectos diagnosticados, re-ejecutados        #");
        System.out.println("#########################################################");

        d01RecargoUrgente();
        d02CantidadNoPositiva();
        d03ReposicionTrasRecuento();
        d04CargarStockPisa();
        d05TipoEnvioSensible();
        d06UmbralEstricto();
        d07NotificacionSilenciosa();
        d08RegistroNoAtomico();
        d09NotacionCientifica();
        d10FacturadoIncluyeNoPagados();
        d11DatosDelClienteCongelados();
        d12DineroEnDouble();
        d13EstadoExpuesto();
        d14OrdenNoDeterminista();
        d15DobleCancelacion();

        int defectosReales = corregidos + atenuados + pendientes;

        System.out.println();
        System.out.println("#########################################################");
        System.out.println("  Defectos confirmados en el diagnostico: " + defectosReales);
        System.out.println("    corregidos ....... " + corregidos);
        System.out.println("    atenuados ........ " + atenuados + "  (defecto abierto)");
        System.out.println("    pendientes ....... " + pendientes);
        System.out.println("  Hipotesis descartadas, sin regresion: " + descartados);
        System.out.println("#########################################################");
        System.out.println();
        if (atenuados > 0) {
            System.out.println("  NOTA PARA EL INFORME: un defecto ATENUADO no es un defecto");
            System.out.println("  corregido. El titular honesto es " + corregidos + " de "
                    + defectosReales + ", no " + defectosReales + " de " + defectosReales + ".");
            System.out.println();
        }
        System.exit(pendientes == 0 ? 0 : 1);
    }

    static void titulo(String t) {
        System.out.println();
        System.out.println("---------------------------------------------------------");
        System.out.println(t);
    }

    static void verificar(String defecto, boolean corregido, String evidencia) {
        if (corregido) {
            corregidos++;
            System.out.println("  [CORREGIDO] " + defecto);
        } else {
            pendientes++;
            System.out.println("  [PENDIENTE] " + defecto);
        }
        System.out.println("              " + evidencia);
    }

    /**
     * El comportamiento numerico sigue siendo el mismo, pero el refactoring
     * redujo el dano: el riesgo ahora es visible o mas dificil de activar.
     *
     * Existe como veredicto separado porque marcarlo CORREGIDO seria mentir en
     * el informe, y marcarlo PENDIENTE a secas perderia la unica mejora real
     * que hubo. Un defecto atenuado sigue siendo un defecto abierto.
     */
    static void atenuar(String defecto, String evidencia) {
        atenuados++;
        System.out.println("  [ATENUADO]  " + defecto);
        System.out.println("              " + evidencia);
    }

    /**
     * Hipotesis que el diagnostico descarto por ejecucion. No es un defecto y
     * no se cuenta como corregido: se re-ejecuta solo para comprobar que el
     * refactoring no la convirtio en uno.
     */
    static void seguirDescartado(String hipotesis, boolean sigueSana, String evidencia) {
        if (sigueSana) {
            descartados++;
            System.out.println("  [NO ERA DEFECTO] " + hipotesis);
        } else {
            pendientes++;
            System.out.println("  [REGRESION] " + hipotesis);
        }
        System.out.println("              " + evidencia);
    }

    static PedidoManager gestor() {
        return new PedidoManager(new NotificadorSilencioso());
    }

    static Direccion enProvincia(String provincia) {
        return new Direccion("Calle 1", "Ciudad", provincia, "5500");
    }

    static String pedir(PedidoManager m, String doc, String provincia, String codigo,
                        int cantidad, TipoEnvio tipo, boolean urgente) {
        return m.registrar(new SolicitudDePedido(doc, "Cliente " + doc, doc + "@e.com",
                "261000", enProvincia(provincia), codigo, cantidad, tipo, "", urgente));
    }

    // ------------------------------------------------------------------
    static void d01RecargoUrgente() {
        titulo("D-01: el recargo por urgencia se evaporaba sobre el umbral");
        PedidoManager m = gestor();
        m.ingresarStock("VIN-MALBEC-750", 100);

        String normal = pedir(m, "1", "Cordoba", "VIN-MALBEC-750", 4, TipoEnvio.ENVIO, false);
        String urgente = pedir(m, "2", "Cordoba", "VIN-MALBEC-750", 4, TipoEnvio.ENVIO, true);
        Dinero tNormal = m.getPedido(normal).total();
        Dinero tUrgente = m.getPedido(urgente).total();

        System.out.println("  sobre el umbral: normal $" + tNormal + ", urgente $" + tUrgente);

        PedidoManager m2 = gestor();
        m2.ingresarStock("VIN-MALBEC-750", 100);
        String n2 = pedir(m2, "1", "Cordoba", "VIN-MALBEC-750", 2, TipoEnvio.ENVIO, false);
        String u2 = pedir(m2, "2", "Cordoba", "VIN-MALBEC-750", 2, TipoEnvio.ENVIO, true);
        System.out.println("  bajo el umbral:  normal $" + m2.getPedido(n2).envio()
                + ", urgente $" + m2.getPedido(u2).envio());

        boolean cobraArriba = tUrgente.compareTo(tNormal) > 0;
        boolean cobraAbajo = m2.getPedido(u2).envio()
                .compareTo(m2.getPedido(n2).envio()) > 0;

        verificar("el urgente no pagaba recargo sobre el umbral",
                cobraArriba && cobraAbajo,
                "El urgente grande ahora paga $" + tUrgente.toString() + " contra $"
                        + tNormal + " del normal. El caso que ya funcionaba bien (bajo el "
                        + "umbral) sigue igual.");
    }

    static void d02CantidadNoPositiva() {
        titulo("D-02: cantidad cero o negativa se aceptaba");
        PedidoManager m = gestor();
        m.ingresarStock("VIN-MALBEC-750", 10);

        String cero = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 0, TipoEnvio.ENVIO, false);
        int antes = m.stockDe("VIN-MALBEC-750");
        String neg = pedir(m, "2", "Mendoza", "VIN-MALBEC-750", -5, TipoEnvio.ENVIO, false);
        int despues = m.stockDe("VIN-MALBEC-750");

        System.out.println("  cantidad 0  -> " + cero);
        System.out.println("  cantidad -5 -> " + neg + ", stock " + antes + " -> " + despues);

        verificar("cantidad <= 0 corrompia stock y contabilidad",
                cero == null && neg == null && antes == despues,
                "Ambos rechazados y el stock quedo en " + despues + ". Antes la cantidad "
                        + "negativa inflaba el inventario de 10 a 15 y metia un subtotal "
                        + "negativo en la facturacion.");
    }

    static void d03ReposicionTrasRecuento() {
        titulo("D-03: cancelar tras un recuento de deposito inflaba el stock");
        PedidoManager m = gestor();
        m.ingresarStock("ACE-OLIVA-500", 30);
        String p = pedir(m, "1", "Mendoza", "ACE-OLIVA-500", 10, TipoEnvio.RETIRO, false);

        m.fijarStock("ACE-OLIVA-500", 100);
        System.out.println("  recuento fisico fija el stock en 100");
        m.cambiarEstado(p, EstadoPedido.CANCELADO);
        int fin = m.stockDe("ACE-OLIVA-500");
        System.out.println("  se cancela el pedido viejo -> " + fin);

        atenuar("la reposicion sigue pisando el recuento del deposito",
                "Quedo en " + fin + ", igual que en el sistema base: el recuento dijo 100 y "
                        + "aparecieron 10 unidades que nadie conto. La aritmetica NO cambio y "
                        + "seria deshonesto contarlo como corregido. Lo unico que mejoro es "
                        + "que ahora las dos intenciones tienen nombres distintos "
                        + "(ingresarStock vs fijarStock), asi que el recuento es una operacion "
                        + "declarada y no un efecto lateral de un metodo ambiguo. La causa de "
                        + "fondo es que el sistema no modela stock RESERVADO: al cancelar "
                        + "repone a ciegas sin saber si su reserva sobrevivio al recuento. "
                        + "Arreglarlo requiere un modelo de reservas, que excede el alcance "
                        + "de este trabajo. Queda como defecto ABIERTO y documentado.");
    }

    static void d04CargarStockPisa() {
        titulo("D-04: cargarStock reemplazaba en vez de sumar");
        PedidoManager m = gestor();
        m.ingresarStock("DUL-MEMBRILLO-400", 50);
        m.ingresarStock("DUL-MEMBRILLO-400", 20);
        int sumado = m.stockDe("DUL-MEMBRILLO-400");

        PedidoManager m2 = gestor();
        m2.ingresarStock("DUL-MEMBRILLO-400", 50);
        m2.fijarStock("DUL-MEMBRILLO-400", 20);
        int fijado = m2.stockDe("DUL-MEMBRILLO-400");

        System.out.println("  ingresarStock(50) + ingresarStock(20) -> " + sumado);
        System.out.println("  ingresarStock(50) + fijarStock(20)    -> " + fijado);

        verificar("un metodo llamado cargar borraba el inventario",
                sumado == 70 && fijado == 20,
                "Las dos intenciones ahora son operaciones distintas con nombres que las "
                        + "distinguen. Antes ambas eran cargarStock y la unica disponible era "
                        + "la destructiva.");
    }

    static void d05TipoEnvioSensible() {
        titulo("D-05: tipoEnvio era sensible a mayusculas y aceptaba basura");
        PedidoManager m = gestor();
        m.ingresarStock("VIN-MALBEC-750", 100);

        String min = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1,
                TipoEnvio.desdeTexto("retiro"), false);
        String esp = pedir(m, "2", "Mendoza", "VIN-MALBEC-750", 1,
                TipoEnvio.desdeTexto("  RETIRO "), false);
        String basura = pedir(m, "3", "Mendoza", "VIN-MALBEC-750", 1,
                TipoEnvio.desdeTexto("TELETRANSPORTE"), false);

        System.out.println("  'retiro'         -> envio $" + m.getPedido(min).envio());
        System.out.println("  '  RETIRO '      -> envio $" + m.getPedido(esp).envio());
        System.out.println("  'TELETRANSPORTE' -> " + basura);

        verificar("escribir 'retiro' en minuscula costaba plata",
                m.getPedido(min).envio().esCero()
                        && m.getPedido(esp).envio().esCero() && basura == null,
                "Las variantes validas se interpretan bien y el valor inexistente se "
                        + "rechaza en vez de entrar al sistema.");
    }

    static void d06UmbralEstricto() {
        titulo("D-06: el umbral de envio gratis usaba > estricto");
        PedidoManager m = gestor();
        m.ingresarStock("BOR-TEST-001", 100);

        String justo = pedir(m, "1", "Mendoza", "BOR-TEST-001", 5, TipoEnvio.ENVIO, false);
        String menos = pedir(m, "2", "Mendoza", "BOR-TEST-001", 4, TipoEnvio.ENVIO, false);

        System.out.println("  subtotal $" + m.getPedido(justo).subtotal()
                + " -> envio $" + m.getPedido(justo).envio());
        System.out.println("  subtotal $" + m.getPedido(menos).subtotal()
                + " -> envio $" + m.getPedido(menos).envio());

        verificar("exactamente $25.000 pagaba flete completo",
                m.getPedido(justo).envio().esCero()
                        && !m.getPedido(menos).envio().esCero(),
                "El umbral es inclusivo y la regla quedo documentada como constante con "
                        + "nombre, no como un numero magico en medio de un metodo largo.");
    }

    static void d07NotificacionSilenciosa() {
        titulo("D-07: un catch vacio ocultaba las fallas de notificacion");
        NotificadorPorEmail notificador = new NotificadorPorEmail();
        PedidoManager m = new PedidoManager(new Inventario(),
                new PoliticaDeCostos(), notificador);
        m.ingresarStock("VIN-MALBEC-750", 100);
        String p = pedir(m, "77", "Mendoza", "VIN-MALBEC-750", 1, TipoEnvio.RETIRO, false);

        // Cliente sin mail: el escenario que antes lanzaba NPE y se perdia.
        PedidoManager m2 = new PedidoManager(new Inventario(),
                new PoliticaDeCostos(), notificador);
        m2.ingresarStock("VIN-MALBEC-750", 100);
        m2.registrar(new SolicitudDePedido("88", "Sin Mail", null, "1",
                enProvincia("Mendoza"), "VIN-MALBEC-750", 1, TipoEnvio.RETIRO, "", false));

        System.out.println("  pedido con cliente sin mail registrado: " + (p != null));
        System.out.println("  fallas registradas: " + notificador.fallas().size());
        for (String f : notificador.fallas()) {
            System.out.println("    - " + f);
        }

        verificar("una notificacion fallida desaparecia sin dejar rastro",
                notificador.huboFallas(),
                "La falla queda registrada y es consultable. La operacion de negocio no se "
                        + "cancela por un mail que no salio, pero tampoco se pierde el dato.");
    }

    static void d08RegistroNoAtomico() {
        titulo("D-08: un pedido rechazado dejaba el cliente dado de alta");
        PedidoManager m = gestor();
        m.ingresarStock("VIN-MALBEC-750", 2);

        String r = pedir(m, "99999", "Mendoza", "VIN-MALBEC-750", 999,
                TipoEnvio.ENVIO, false);
        boolean quedo = m.getCliente("99999") != null;

        System.out.println("  pedido sin stock -> " + r);
        System.out.println("  cliente persistido -> " + quedo);
        System.out.println("  pedidos registrados -> " + m.todos().size());

        verificar("registrarPedido no era atomico",
                r == null && !quedo && m.todos().isEmpty(),
                "Se valida todo antes de escribir cualquier cosa. Un pedido que nunca "
                        + "existio ya no deja residuo en la base de clientes.");
    }

    static void d09NotacionCientifica() {
        titulo("D-09: el reporte contable salia en notacion cientifica");
        PedidoManager m = gestor();
        m.ingresarStock("VIN-MALBEC-750", 5000);
        String p = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 2000, TipoEnvio.RETIRO, false);
        m.cambiarEstado(p, EstadoPedido.PAGADO);

        String linea = "";
        for (String l : m.reporte().split("\n")) {
            if (l.contains("Facturado")) {
                linea = l;
            }
        }
        System.out.println("  " + linea);

        verificar("el reporte mostraba $1.7E7",
                linea.contains("17.000.000,00"),
                "Formato de moneda argentino. Antes la concatenacion de double delegaba "
                        + "en Double.toString() y pasaba a notacion cientifica desde 10^7.");
    }

    static void d10FacturadoIncluyeNoPagados() {
        titulo("D-10: 'Facturado' sumaba pedidos que nadie pago");
        PedidoManager m = gestor();
        m.ingresarStock("VIN-MALBEC-750", 100);

        pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1, TipoEnvio.RETIRO, false);
        Dinero soloPendiente = m.totalFacturado();

        String pagado = pedir(m, "2", "Mendoza", "VIN-MALBEC-750", 1, TipoEnvio.RETIRO, false);
        m.cambiarEstado(pagado, EstadoPedido.PAGADO);
        Dinero conPagado = m.totalFacturado();

        System.out.println("  solo un PENDIENTE   -> facturado $" + soloPendiente);
        System.out.println("  agregando un PAGADO -> facturado $" + conPagado);

        verificar("un pedido recien ingresado ya contaba como facturacion",
                soloPendiente.esCero() && conPagado.equals(Dinero.de(8500)),
                "El criterio quedo explicito en EstadoPedido.cuentaComoFacturado(), donde "
                        + "se puede discutir con el area contable en vez de estar implicito "
                        + "en una negacion dentro de un for.");
    }

    static void d11DatosDelClienteCongelados() {
        titulo("D-11: los datos del cliente quedaban congelados en el primer alta");
        PedidoManager m = gestor();
        m.ingresarStock("DUL-MEMBRILLO-400", 100);

        m.registrar(new SolicitudDePedido("30111222", "Maria Fernandez", "maria@viejo.com",
                "2615550001", new Direccion("San Martin 100", "Mendoza", "Mendoza", "5500"),
                "DUL-MEMBRILLO-400", 1, TipoEnvio.ENVIO, "", false));
        String p2 = m.registrar(new SolicitudDePedido("30111222", "Maria Fernandez",
                "maria@nuevo.com", "3515559999",
                new Direccion("Colon 2000", "Cordoba", "Cordoba", "5000"),
                "DUL-MEMBRILLO-400", 1, TipoEnvio.ENVIO, "", false));

        Cliente c = m.getCliente("30111222");
        System.out.println("  mail guardado:      " + c.email());
        System.out.println("  domicilio guardado: " + c.direccion());
        System.out.println("  flete cobrado:      $" + m.getPedido(p2).envio());

        boolean coherente = c.email().equals("maria@nuevo.com")
                && c.provincia().equals("Cordoba")
                && m.getPedido(p2).envio().equals(Dinero.de(4500));

        verificar("se cobraba un destino y se enviaba a otro",
                coherente,
                "El domicilio guardado y la tarifa cobrada corresponden ahora a la misma "
                        + "provincia. Antes se cobraba el flete de Cordoba y el paquete salia "
                        + "al domicilio viejo de Mendoza.");
    }

    static void d12DineroEnDouble() {
        titulo("D-12: el dinero en double acumulaba error");
        Dinero suma = Dinero.CERO;
        for (int i = 0; i < 10; i++) {
            suma = suma.mas(Dinero.de("1234.10"));
        }
        double enDouble = 0;
        for (int i = 0; i < 10; i++) {
            enDouble += 1234.10;
        }

        System.out.println("  con double  -> " + enDouble);
        System.out.println("  con Dinero  -> " + suma);

        verificar("la aritmetica de dinero arrastraba error de representacion",
                suma.equals(Dinero.de("12341.00")),
                "BigDecimal con 2 decimales y redondeo HALF_UP. El double daba "
                        + enDouble + " donde la respuesta exacta es 12341.00.");
    }

    static void d13EstadoExpuesto() {
        titulo("D-13: el estado interno era publico y mutable");
        PedidoManager m = gestor();
        m.ingresarStock("VIN-MALBEC-750", 10);
        String n = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1, TipoEnvio.RETIRO, false);

        // stock() devuelve una COPIA defensiva, no una vista inmutable: el put
        // tiene exito sobre la copia. Lo que hay que verificar no es que lance
        // excepcion sino que el inventario real quede intacto. Comparar contra
        // ">= 0" seria una asercion floja que pasa aunque el ataque funcione.
        int stockEsperado = m.stockDe("VIN-MALBEC-750");
        boolean stockProtegido;
        try {
            m.stock().put("VIN-MALBEC-750", -999);
            stockProtegido = m.stockDe("VIN-MALBEC-750") == stockEsperado;
        } catch (UnsupportedOperationException e) {
            stockProtegido = true;
        }

        boolean pedidosProtegidos;
        try {
            m.pedidos().remove(n);
            pedidosProtegidos = m.getPedido(n) != null;
        } catch (UnsupportedOperationException e) {
            pedidosProtegidos = true;
        }

        // La transicion invalida se pide por el manager, que es el unico camino
        // disponible desde afuera del paquete.
        //
        // Esta sonda vive en el paquete `sondas`, no en `pedidos`, y eso ahora
        // importa: la linea que antes decia
        //
        //     p.cambiarEstadoA(EstadoPedido.ENTREGADO)
        //
        // YA NO COMPILA. El compilador la rechaza con "cambiarEstadoA is not
        // public in Pedido; cannot be accessed from outside package". Ese error
        // de compilacion es, en si mismo, la verificacion mas fuerte del
        // encapsulamiento: el agujero no esta cerrado por una validacion en
        // tiempo de ejecucion que alguien podria saltear, sino por el sistema
        // de tipos. Codigo externo que intente cancelar un pedido por afuera
        // del manager no llega a ejecutarse porque no llega a compilar.
        boolean transicionProtegida = !m.cambiarEstado(n, EstadoPedido.ENTREGADO);

        System.out.println("  intento de stock -999      -> stock real "
                + m.stockDe("VIN-MALBEC-750") + " (esperado " + stockEsperado + ")");
        System.out.println("  intento de borrar el pedido -> sigue existiendo "
                + (m.getPedido(n) != null));
        System.out.println("  salto de estado invalido    -> rechazado "
                + transicionProtegida);

        verificar("se podia poner el stock en -999 y el estado en 'INVENTADO'",
                stockProtegido && pedidosProtegidos && transicionProtegida,
                "Las colecciones salen protegidas por dos mecanismos distintos: pedidos y "
                        + "clientes como vista inmutable (el put lanza excepcion) y el stock "
                        + "como copia defensiva (el put funciona, pero sobre la copia y no "
                        + "toca el inventario real). El estado solo cambia por transiciones "
                        + "validas, y un estado inexistente ya ni siquiera compila porque "
                        + "EstadoPedido es un enum.");
    }

    static void d14OrdenNoDeterminista() {
        titulo("D-14: el orden del reporte no era determinista");
        PedidoManager m = gestor();
        m.ingresarStock("VIN-MALBEC-750", 500);
        for (int i = 1; i <= 6; i++) {
            pedir(m, "doc" + i, "Mendoza", "VIN-MALBEC-750", 1, TipoEnvio.RETIRO, false);
        }

        StringBuilder orden = new StringBuilder();
        for (String l : m.reporte().split("\n")) {
            if (l.startsWith("PED-")) {
                orden.append(l.substring(0, l.indexOf(' '))).append(" ");
            }
        }
        String obtenido = orden.toString().trim();
        System.out.println("  orden del reporte: " + obtenido);

        List<Pedido> b1 = m.buscar(new FiltroDePedidos());
        List<Pedido> b2 = m.buscar(new FiltroDePedidos());
        boolean busquedaEstable = b1.get(0).numero().equals(b2.get(0).numero());

        verificar("el listado administrativo cambiaba de orden sin motivo",
                obtenido.equals("PED-1 PED-2 PED-3 PED-4 PED-5 PED-6") && busquedaEstable,
                "LinkedHashMap garantiza el orden de insercion. Antes el recorrido de "
                        + "keySet() sobre un HashMap devolvia 5,4,3,2,1,6.");
    }

    static void d15DobleCancelacion() {
        titulo("D-15: hipotesis DESCARTADA en el diagnostico - sigue descartada?");
        PedidoManager m = gestor();
        m.ingresarStock("ACE-OLIVA-500", 10);
        String p = pedir(m, "1", "Mendoza", "ACE-OLIVA-500", 5, TipoEnvio.RETIRO, false);

        m.cambiarEstado(p, EstadoPedido.CANCELADO);
        int trasUna = m.stockDe("ACE-OLIVA-500");
        boolean segunda = m.cambiarEstado(p, EstadoPedido.CANCELADO);
        int trasDos = m.stockDe("ACE-OLIVA-500");

        System.out.println("  tras cancelar: " + trasUna + ", tras recancelar: " + trasDos);
        System.out.println("  la segunda cancelacion devolvio: " + segunda);

        seguirDescartado("cancelar dos veces repondria stock dos veces",
                !segunda && trasUna == trasDos && trasDos == 10,
                "El sistema base YA estaba protegido aca y se dejo asentado en el "
                        + "diagnostico para no reportar un defecto inexistente. El "
                        + "refactoring tampoco lo rompio: CANCELADO es terminal por "
                        + "definicion del enum, no por una rama del if.");
    }
}
