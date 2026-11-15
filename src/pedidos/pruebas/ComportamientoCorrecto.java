package pedidos.pruebas;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import pedidos.Dinero;
import pedidos.Direccion;
import pedidos.EstadoPedido;
import pedidos.FiltroDePedidos;
import pedidos.NotificadorSilencioso;
import pedidos.SolicitudDePedido;
import pedidos.TipoEnvio;
import pedidos.Pedido;
import pedidos.PedidoManager;

/**
 * PRUEBAS DE COMPORTAMIENTO CORRECTO.
 *
 * Estas pruebas dicen como DEBERIA comportarse el sistema. Hoy, contra el
 * codigo base, varias FALLAN. Esa falla es el punto: cada caso rojo es un
 * defecto confirmado por la sonda de diagnostico, traducido a una prueba
 * ejecutable.
 *
 * Son lo opuesto a CaracterizacionBase, y la pareja es deliberada:
 *
 *   CaracterizacionBase ..... pasa HOY y debe seguir pasando siempre.
 *                             Si se pone en rojo, el refactoring rompio algo.
 *   ComportamientoCorrecto .. falla HOY y debe ponerse en verde.
 *                             Cada caso que pasa a verde es un defecto corregido.
 *
 * Ese contraste es la evidencia del trabajo: el antes y el despues medidos,
 * no narrados.
 *
 * REGLAS DE NEGOCIO QUE SE ASUMEN (y que en un caso real habria que confirmar
 * con el cliente antes de tocar nada):
 *
 *   R1. El envio bonificado por compra grande cubre el FLETE ESTANDAR. El
 *       servicio urgente es un adicional por logistica prioritaria y se cobra
 *       siempre: 50% de la tarifa de la zona.
 *         flete = (bonificado ? 0 : tarifa) + (urgente ? tarifa * 0.5 : 0)
 *       Esta formula deja intacto el caso que hoy ya funciona (bajo el umbral,
 *       urgente paga tarifa * 1.5) y solo corrige el caso roto.
 *   R2. El umbral de bonificacion es "desde $25.000", inclusive.
 *   R3. "Facturado" es lo efectivamente cobrado: pedidos de PAGADO en adelante.
 *       Un PENDIENTE es expectativa, no facturacion.
 *   R4. Un pedido debe tener cantidad mayor a cero.
 *   R5. registrarPedido es atomico: si se rechaza, no deja rastro.
 *
 * ponytail: los casos que necesitan API que todavia no existe (fijarStock para
 * recuento de deposito, consulta de fallas de notificacion) se incorporan en la
 * etapa de refactoring, cuando esa API exista. Se dejan anotados mas abajo para
 * no perderlos.
 */
public class ComportamientoCorrecto {

    public static void main(String[] args) {
        Verificador v = new Verificador("COMPORTAMIENTO CORRECTO ESPERADO");

        recargoUrgenteSiempreSeCobra(v);
        umbralInclusivo(v);
        cantidadDebeSerPositiva(v);
        registroAtomico(v);
        cargarStockAcumula(v);
        tipoEnvioRobusto(v);
        datosDelClienteSeActualizan(v);
        facturadoSoloCuentaCobrado(v);
        importesConFormatoDeMoneda(v);
        ordenDeterminista(v);
        estadoInternoEncapsulado(v);
        defectosDetectadosEnLaRevision(v);

        boolean ok = v.resumen();
        System.out.println();
        if (ok) {
            System.out.println("  Todos los defectos conocidos estan corregidos.");
        } else {
            System.out.println("  Las fallas de arriba son los defectos PENDIENTES de corregir.");
            System.out.println("  Contra el sistema base, esto es lo esperado.");
        }
        CaracterizacionBase.salir(args, ok);
    }

    static PedidoManager conStock() {
        PedidoManager m = new PedidoManager(new NotificadorSilencioso());
        m.ingresarStock("VIN-MALBEC-750", 500);
        m.ingresarStock("ACE-OLIVA-500", 500);
        m.ingresarStock("DUL-MEMBRILLO-400", 500);
        m.ingresarStock("FRU-NUEZ-250", 500);
        m.ingresarStock("OTR-GENERICO-1", 500);
        return m;
    }

    static String pedir(PedidoManager m, String doc, String provincia, String codigo,
                        int cantidad, String tipoEnvio, boolean urgente) {
        return m.registrar(new SolicitudDePedido(doc, "Cliente " + doc, doc + "@ejemplo.com",
                "261000", new Direccion("Calle 1", "Ciudad", provincia, "5500"),
                codigo, cantidad, TipoEnvio.desdeTexto(tipoEnvio), "", urgente));
    }

    static void recargoUrgenteSiempreSeCobra(Verificador v) {
        PedidoManager m = conStock();

        // Bajo el umbral: 2 vinos = 17000. Tarifa Cordoba 4500.
        String chicoNormal = pedir(m, "1", "Cordoba", "VIN-MALBEC-750", 2, "ENVIO", false);
        String chicoUrgente = pedir(m, "2", "Cordoba", "VIN-MALBEC-750", 2, "ENVIO", true);
        v.igual("bajo el umbral el flete normal es la tarifa de zona", Dinero.de(4500), m.getPedido(chicoNormal).envio());
        v.igual("bajo el umbral el urgente paga tarifa mas 50%", Dinero.de(6750), m.getPedido(chicoUrgente).envio());

        // Sobre el umbral: 4 vinos = 34000. El flete estandar se bonifica.
        String grandeNormal = pedir(m, "3", "Cordoba", "VIN-MALBEC-750", 4, "ENVIO", false);
        String grandeUrgente = pedir(m, "4", "Cordoba", "VIN-MALBEC-750", 4, "ENVIO", true);
        v.igual("sobre el umbral el flete estandar se bonifica", Dinero.de(0), m.getPedido(grandeNormal).envio());
        v.igual("sobre el umbral el urgente igual paga el adicional de urgencia", Dinero.de(2250), m.getPedido(grandeUrgente).envio());

        v.cierto("un pedido urgente nunca cuesta lo mismo que uno normal",
                m.getPedido(grandeUrgente).total()
                        .compareTo(m.getPedido(grandeNormal).total()) > 0);
    }

    /** R2. El umbral debe ser inclusivo: "desde $25.000". */
    static void umbralInclusivo(Verificador v) {
        PedidoManager m = conStock();
        String justo = pedir(m, "1", "Mendoza", "OTR-GENERICO-1", 5, "ENVIO", false);
        v.igual("el subtotal de prueba es exactamente el umbral", Dinero.de(25000), m.getPedido(justo).subtotal());
        v.igual("un subtotal de exactamente 25000 ya tiene envio bonificado", Dinero.de(0), m.getPedido(justo).envio());
    }

    /** R4. Cantidad cero o negativa debe rechazarse. */
    static void cantidadDebeSerPositiva(Verificador v) {
        PedidoManager m = conStock();

        v.igual("un pedido de cantidad cero se rechaza", null,
                pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 0, "ENVIO", false));

        int antes = m.stockDe("VIN-MALBEC-750");
        v.igual("un pedido de cantidad negativa se rechaza", null,
                pedir(m, "2", "Mendoza", "VIN-MALBEC-750", -5, "ENVIO", false));
        v.igual("una cantidad negativa no altera el stock", antes, m.stockDe("VIN-MALBEC-750"));

        v.cierto("ningun pedido registrado tiene importe negativo",
                m.buscar(new FiltroDePedidos()).stream().noneMatch(p -> p.total().esNegativo()));
    }

    /** R5. Un pedido rechazado no debe dejar residuo. */
    static void registroAtomico(Verificador v) {
        PedidoManager m = new PedidoManager(new NotificadorSilencioso());
        m.ingresarStock("VIN-MALBEC-750", 2);

        String r = pedir(m, "99999", "Mendoza", "VIN-MALBEC-750", 999, "ENVIO", false);
        v.igual("el pedido sin stock se rechaza", null, r);
        v.falso("un pedido rechazado no da de alta al cliente",
                m.getCliente("99999") != null);
        v.igual("un pedido rechazado no deja pedidos registrados",
                0, m.buscar(new FiltroDePedidos()).size());
    }

    /** cargarStock debe sumar el ingreso, no reemplazar el inventario. */
    static void cargarStockAcumula(Verificador v) {
        PedidoManager m = new PedidoManager(new NotificadorSilencioso());
        m.ingresarStock("DUL-MEMBRILLO-400", 50);
        m.ingresarStock("DUL-MEMBRILLO-400", 20);
        v.igual("dos ingresos de mercaderia se acumulan",
                70, m.stockDe("DUL-MEMBRILLO-400"));
    }

    /** El tipo de envio no debe depender de como se escriba, ni aceptar basura. */
    static void tipoEnvioRobusto(Verificador v) {
        PedidoManager m = conStock();

        String minuscula = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1, "retiro", false);
        v.cierto("'retiro' en minuscula se acepta", minuscula != null);
        if (minuscula != null) {
            v.igual("'retiro' en minuscula no paga flete", Dinero.de(0), m.getPedido(minuscula).envio());
        }

        String conEspacios = pedir(m, "2", "Mendoza", "VIN-MALBEC-750", 1, "  RETIRO ", false);
        v.cierto("el tipo de envio tolera espacios alrededor", conEspacios != null);
        if (conEspacios != null) {
            v.igual("el tipo con espacios no paga flete", Dinero.de(0), m.getPedido(conEspacios).envio());
        }

        v.igual("un tipo de envio inexistente se rechaza", null,
                pedir(m, "3", "Mendoza", "VIN-MALBEC-750", 1, "TELETRANSPORTE", false));
    }

    /** Los datos del cliente deben reflejar el ultimo pedido, no el primero. */
    static void datosDelClienteSeActualizan(Verificador v) {
        PedidoManager m = conStock();

        m.registrar(new SolicitudDePedido("30111222", "Maria Fernandez", "maria@viejo.com",
                "2615550001", new Direccion("San Martin 100", "Mendoza", "Mendoza", "5500"),
                "DUL-MEMBRILLO-400", 1, TipoEnvio.ENVIO, "", false));
        m.registrar(new SolicitudDePedido("30111222", "Maria Fernandez", "maria@nuevo.com",
                "3515559999", new Direccion("Colon 2000", "Cordoba", "Cordoba", "5000"),
                "DUL-MEMBRILLO-400", 1, TipoEnvio.ENVIO, "", false));

        v.igual("el mail del cliente se actualiza",
                "maria@nuevo.com", m.getCliente("30111222").email());
        v.igual("la provincia del cliente se actualiza",
                "Cordoba", m.getCliente("30111222").provincia());
        v.igual("la calle del cliente se actualiza",
                "Colon 2000", m.getCliente("30111222").direccion().calle());
    }

    /** R3. "Facturado" no debe incluir pedidos que nadie pago. */
    static void facturadoSoloCuentaCobrado(Verificador v) {
        PedidoManager m = conStock();

        pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        v.igual("un pedido pendiente no cuenta como facturado", Dinero.de(0), m.totalFacturado());

        String pagado = pedir(m, "2", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        m.cambiarEstado(pagado, "PAGADO");
        v.igual("un pedido pagado cuenta como facturado", Dinero.de(8500), m.totalFacturado());

        String cancelado = pedir(m, "3", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        m.cambiarEstado(cancelado, "CANCELADO");
        v.igual("un pedido cancelado no cuenta como facturado", Dinero.de(8500), m.totalFacturado());
    }

    /** Un reporte contable no puede salir en notacion cientifica. */
    static void importesConFormatoDeMoneda(Verificador v) {
        PedidoManager m = new PedidoManager(new NotificadorSilencioso());
        m.ingresarStock("VIN-MALBEC-750", 5000);
        String p = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 2000, "RETIRO", false);
        m.cambiarEstado(p, "PAGADO");

        String rep = m.reporte();
        v.falso("el reporte no usa notacion cientifica",
                rep.contains("E7") || rep.contains("E8") || rep.contains("E9"));
        v.cierto("el reporte formatea el importe con separador de miles",
                rep.contains("17.000.000,00"));
    }

    /** Un listado administrativo tiene que salir siempre en el mismo orden. */
    static void ordenDeterminista(Verificador v) {
        PedidoManager m = new PedidoManager(new NotificadorSilencioso());
        m.ingresarStock("VIN-MALBEC-750", 500);
        for (int i = 1; i <= 6; i++) {
            pedir(m, "doc" + i, "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        }

        StringBuilder orden = new StringBuilder();
        for (String linea : m.reporte().split("\n")) {
            if (linea.startsWith("PED-")) {
                orden.append(linea.substring(0, linea.indexOf(' '))).append(" ");
            }
        }
        v.igual("el reporte sale en orden de creacion",
                "PED-1 PED-2 PED-3 PED-4 PED-5 PED-6", orden.toString().trim());

        List<Pedido> r1 = m.buscar(new FiltroDePedidos());
        List<Pedido> r2 = m.buscar(new FiltroDePedidos());
        StringBuilder o1 = new StringBuilder();
        StringBuilder o2 = new StringBuilder();
        for (Pedido p : r1) o1.append(p.numero()).append(" ");
        for (Pedido p : r2) o2.append(p.numero()).append(" ");
        v.igual("dos busquedas identicas devuelven el mismo orden",
                o1.toString(), o2.toString());
    }

    /**
     * Defectos encontrados por la REVISION DE CODIGO del sistema refactorizado.
     *
     * Estos casos no vienen del diagnostico del sistema base: son defectos que
     * el propio refactoring introdujo o reabrio mientras corregia otros. Esa es
     * la leccion mas incomoda del trabajo, y por eso quedan documentados aparte
     * en vez de mezclados con los anteriores: refactorizar con una red de
     * pruebas reduce el riesgo, no lo elimina. La red cubre lo que alguien
     * penso en cubrir.
     *
     * Los cinco fueron verificados ejecutando codigo antes de corregirlos, y
     * estos casos existen para que no vuelvan.
     */
    static void defectosDetectadosEnLaRevision(Verificador v) {
        datosDeContactoNoSeBorran(v);
        busquedaNoPuedeMutarLaBase(v);
        provinciaSeComparaIgualEnTodosLados(v);
        elPedidoGuardaSuDestino(v);
        coleccionesExpuestasSonInmutables(v);
    }

    /**
     * C-1. Un pedido valido que no trae datos de contacto NO debe borrarlos.
     *
     * Defecto introducido al corregir el de los datos congelados: la
     * actualizacion paso a ser incondicional y, como la solicitud no exige
     * nombre ni email, un pedido legitimo sin esos campos vaciaba el cliente.
     * Al quedarse sin mail dejaba de recibir toda notificacion futura.
     */
    static void datosDeContactoNoSeBorran(Verificador v) {
        PedidoManager m = conStock();
        m.registrar(new SolicitudDePedido("1", "Maria", "maria@x.com", "261",
                new Direccion("Calle 1", "Ciudad", "Mendoza", "5500"),
                "VIN-MALBEC-750", 1, TipoEnvio.RETIRO, "", false));
        m.registrar(new SolicitudDePedido("1", null, null, null,
                new Direccion("Calle 2", "Ciudad", "Mendoza", "5500"),
                "VIN-MALBEC-750", 1, TipoEnvio.RETIRO, "", false));

        v.igual("un pedido sin nombre no borra el nombre del cliente",
                "Maria", m.getCliente("1").nombre());
        v.igual("un pedido sin mail no borra el mail del cliente",
                "maria@x.com", m.getCliente("1").email());
        v.igual("pero la direccion que SI trae se actualiza",
                "Calle 2", m.getCliente("1").direccion().calle());
    }

    /** C-3. Una consulta de solo lectura no puede modificar la base. */
    static void busquedaNoPuedeMutarLaBase(Verificador v) {
        PedidoManager m = conStock();
        m.registrar(new SolicitudDePedido("1", "A", "a@x.com", "2",
                new Direccion("Calle 1", "Ciudad", "Mendoza", "5500"),
                "VIN-MALBEC-750", 1, TipoEnvio.RETIRO, "", false));

        FiltroHostil hostil = new FiltroHostil();
        m.buscar(hostil);

        boolean protegido;
        try {
            hostil.capturado.clear();
            protegido = false;
        } catch (UnsupportedOperationException e) {
            protegido = true;
        }
        v.cierto("el filtro no puede vaciar el mapa de clientes", protegido);
        v.igual("el cliente sigue existiendo despues de la busqueda",
                1, m.clientes().size());
    }

    /**
     * Subclase hostil: el filtro es un punto de extension publico, asi que
     * cualquiera puede escribir esto. La prueba existe porque el refactoring
     * creo ese punto de extension y hay que probar que esta protegido.
     */
    static class FiltroHostil extends FiltroDePedidos {
        java.util.Map<String, pedidos.Cliente> capturado;

        @Override
        public boolean aceptaA(Pedido p, java.util.Map<String, pedidos.Cliente> clientes) {
            this.capturado = clientes;
            return super.aceptaA(p, clientes);
        }
    }

    /**
     * C-4. El sistema no puede cobrar segun una regla y listar segun otra.
     *
     * ZonaEnvio normalizaba la provincia (trim + ignore case) y el filtro de
     * busqueda no. Un pedido cargado como "mendoza" se cobraba con tarifa de
     * Mendoza pero no aparecia al buscar "Mendoza".
     */
    static void provinciaSeComparaIgualEnTodosLados(Verificador v) {
        PedidoManager m = conStock();
        String n = m.registrar(new SolicitudDePedido("1", "A", "a@x.com", "2",
                new Direccion("Calle 1", "Ciudad", "mendoza", "5500"),
                "VIN-MALBEC-750", 1, TipoEnvio.ENVIO, "", false));

        v.igual("se cobra con tarifa de Mendoza", Dinero.de(1200), m.getPedido(n).envio());
        v.igual("y aparece al buscar 'Mendoza'", 1,
                m.buscar(new FiltroDePedidos().enProvincia("Mendoza")).size());
        v.igual("y tambien al buscar '  mendoza  '", 1,
                m.buscar(new FiltroDePedidos().enProvincia("  mendoza  ")).size());
    }

    /**
     * I-1. Un pedido historico conserva el destino con el que se cotizo.
     *
     * Antes el destino se leia del Cliente, que es mutable: un cliente que
     * pedia a Mendoza y despues a Cordoba dejaba el primer pedido facturado
     * con flete de Mendoza pero listado como de Cordoba.
     */
    static void elPedidoGuardaSuDestino(Verificador v) {
        PedidoManager m = conStock();
        String p1 = m.registrar(new SolicitudDePedido("1", "Maria", "m@x.com", "2",
                new Direccion("Calle 1", "Ciudad", "Mendoza", "5500"),
                "VIN-MALBEC-750", 1, TipoEnvio.ENVIO, "", false));
        String p2 = m.registrar(new SolicitudDePedido("1", "Maria", "m@x.com", "2",
                new Direccion("Calle 2", "Ciudad", "Cordoba", "5000"),
                "VIN-MALBEC-750", 1, TipoEnvio.ENVIO, "", false));

        v.igual("el primer pedido conserva su provincia de entrega",
                "Mendoza", m.getPedido(p1).provinciaDeEntrega());
        v.igual("el segundo tiene la suya", "Cordoba", m.getPedido(p2).provinciaDeEntrega());
        v.igual("el pedido cotizado a Mendoza se lista en Mendoza", 1,
                m.buscar(new FiltroDePedidos().enProvincia("Mendoza")).size());
        v.igual("y el de Cordoba en Cordoba", 1,
                m.buscar(new FiltroDePedidos().enProvincia("Cordoba")).size());
        v.cierto("cada uno conserva el flete con el que se cobro",
                m.getPedido(p1).envio().equals(Dinero.de(1200))
                        && m.getPedido(p2).envio().equals(Dinero.de(4500)));
    }

    /**
     * I-2. Las tres colecciones expuestas tienen el mismo contrato.
     *
     * stock() devolvia una copia mutable: el put se aceptaba en silencio y
     * quien lo hacia creia haber cambiado el inventario.
     */
    static void coleccionesExpuestasSonInmutables(Verificador v) {
        PedidoManager m = conStock();
        m.registrar(new SolicitudDePedido("1", "A", "a@x.com", "2",
                new Direccion("Calle 1", "Ciudad", "Mendoza", "5500"),
                "VIN-MALBEC-750", 1, TipoEnvio.RETIRO, "", false));

        v.cierto("stock() rechaza la escritura", lanzaAlEscribir(() -> m.stock().put("X", -999)));
        v.cierto("pedidos() rechaza la escritura",
                lanzaAlEscribir(() -> m.pedidos().remove("PED-1")));
        v.cierto("clientes() rechaza la escritura",
                lanzaAlEscribir(() -> m.clientes().clear()));
        v.igual("y el estado real queda intacto", 499, m.stockDe("VIN-MALBEC-750"));
    }

    static boolean lanzaAlEscribir(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    /**
     * Las invariantes del dominio no pueden depender de la buena voluntad del
     * llamador. Se verifica por reflexion: ningun campo publico mutable.
     */
    static void estadoInternoEncapsulado(Verificador v) {
        for (Class<?> c : new Class<?>[]{PedidoManager.class, Pedido.class, pedidos.Cliente.class}) {
            StringBuilder expuestos = new StringBuilder();
            for (Field f : c.getDeclaredFields()) {
                boolean publico = Modifier.isPublic(f.getModifiers());
                boolean constante = Modifier.isFinal(f.getModifiers())
                        && Modifier.isStatic(f.getModifiers());
                if (publico && !constante) {
                    expuestos.append(f.getName()).append(" ");
                }
            }
            v.igual(c.getSimpleName() + " no expone campos publicos mutables",
                    "", expuestos.toString().trim());
        }
    }
}
