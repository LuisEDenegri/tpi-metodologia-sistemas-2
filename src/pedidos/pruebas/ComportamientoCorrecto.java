package pedidos.pruebas;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
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
        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 500);
        m.cargarStock("ACE-OLIVA-500", 500);
        m.cargarStock("DUL-MEMBRILLO-400", 500);
        m.cargarStock("OTR-GENERICO-1", 500);
        return m;
    }

    static String pedir(PedidoManager m, String doc, String provincia, String codigo,
                        int cantidad, String tipoEnvio, boolean urgente) {
        return m.registrarPedido(doc, "Cliente " + doc, doc + "@ejemplo.com", "261000",
                "Calle 1", "Ciudad", provincia, "5500", codigo, cantidad,
                tipoEnvio, "", urgente);
    }

    // ------------------------------------------------------------------
    /** R1. Defecto principal: el recargo por urgencia desaparece sobre el umbral. */
    static void recargoUrgenteSiempreSeCobra(Verificador v) {
        PedidoManager m = conStock();

        // Bajo el umbral: 2 vinos = 17000. Tarifa Cordoba 4500.
        String chicoNormal = pedir(m, "1", "Cordoba", "VIN-MALBEC-750", 2, "ENVIO", false);
        String chicoUrgente = pedir(m, "2", "Cordoba", "VIN-MALBEC-750", 2, "ENVIO", true);
        v.igual("bajo el umbral el flete normal es la tarifa de zona",
                4500.0, m.getPedido(chicoNormal).envio);
        v.igual("bajo el umbral el urgente paga tarifa mas 50%",
                6750.0, m.getPedido(chicoUrgente).envio);

        // Sobre el umbral: 4 vinos = 34000. El flete estandar se bonifica.
        String grandeNormal = pedir(m, "3", "Cordoba", "VIN-MALBEC-750", 4, "ENVIO", false);
        String grandeUrgente = pedir(m, "4", "Cordoba", "VIN-MALBEC-750", 4, "ENVIO", true);
        v.igual("sobre el umbral el flete estandar se bonifica",
                0.0, m.getPedido(grandeNormal).envio);
        v.igual("sobre el umbral el urgente igual paga el adicional de urgencia",
                2250.0, m.getPedido(grandeUrgente).envio);

        v.cierto("un pedido urgente nunca cuesta lo mismo que uno normal",
                m.getPedido(grandeUrgente).total > m.getPedido(grandeNormal).total);
    }

    /** R2. El umbral debe ser inclusivo: "desde $25.000". */
    static void umbralInclusivo(Verificador v) {
        PedidoManager m = conStock();
        String justo = pedir(m, "1", "Mendoza", "OTR-GENERICO-1", 5, "ENVIO", false);
        v.igual("el subtotal de prueba es exactamente el umbral",
                25000.0, m.getPedido(justo).subtotal);
        v.igual("un subtotal de exactamente 25000 ya tiene envio bonificado",
                0.0, m.getPedido(justo).envio);
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
                m.buscar(null, null, null, null, null).stream().noneMatch(p -> p.total < 0));
    }

    /** R5. Un pedido rechazado no debe dejar residuo. */
    static void registroAtomico(Verificador v) {
        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 2);

        String r = pedir(m, "99999", "Mendoza", "VIN-MALBEC-750", 999, "ENVIO", false);
        v.igual("el pedido sin stock se rechaza", null, r);
        v.falso("un pedido rechazado no da de alta al cliente",
                m.clientes.containsKey("99999"));
        v.igual("un pedido rechazado no deja pedidos registrados",
                0, m.buscar(null, null, null, null, null).size());
    }

    /** cargarStock debe sumar el ingreso, no reemplazar el inventario. */
    static void cargarStockAcumula(Verificador v) {
        PedidoManager m = new PedidoManager();
        m.cargarStock("DUL-MEMBRILLO-400", 50);
        m.cargarStock("DUL-MEMBRILLO-400", 20);
        v.igual("dos ingresos de mercaderia se acumulan",
                70, m.stockDe("DUL-MEMBRILLO-400"));
    }

    /** El tipo de envio no debe depender de como se escriba, ni aceptar basura. */
    static void tipoEnvioRobusto(Verificador v) {
        PedidoManager m = conStock();

        String minuscula = pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1, "retiro", false);
        v.cierto("'retiro' en minuscula se acepta", minuscula != null);
        if (minuscula != null) {
            v.igual("'retiro' en minuscula no paga flete",
                    0.0, m.getPedido(minuscula).envio);
        }

        String conEspacios = pedir(m, "2", "Mendoza", "VIN-MALBEC-750", 1, "  RETIRO ", false);
        v.cierto("el tipo de envio tolera espacios alrededor", conEspacios != null);
        if (conEspacios != null) {
            v.igual("el tipo con espacios no paga flete",
                    0.0, m.getPedido(conEspacios).envio);
        }

        v.igual("un tipo de envio inexistente se rechaza", null,
                pedir(m, "3", "Mendoza", "VIN-MALBEC-750", 1, "TELETRANSPORTE", false));
    }

    /** Los datos del cliente deben reflejar el ultimo pedido, no el primero. */
    static void datosDelClienteSeActualizan(Verificador v) {
        PedidoManager m = conStock();

        m.registrarPedido("30111222", "Maria Fernandez", "maria@viejo.com", "2615550001",
                "San Martin 100", "Mendoza", "Mendoza", "5500",
                "DUL-MEMBRILLO-400", 1, "ENVIO", "", false);
        m.registrarPedido("30111222", "Maria Fernandez", "maria@nuevo.com", "3515559999",
                "Colon 2000", "Cordoba", "Cordoba", "5000",
                "DUL-MEMBRILLO-400", 1, "ENVIO", "", false);

        v.igual("el mail del cliente se actualiza",
                "maria@nuevo.com", m.clientes.get("30111222").email);
        v.igual("la provincia del cliente se actualiza",
                "Cordoba", m.clientes.get("30111222").provincia);
        v.igual("la calle del cliente se actualiza",
                "Colon 2000", m.clientes.get("30111222").calle);
    }

    /** R3. "Facturado" no debe incluir pedidos que nadie pago. */
    static void facturadoSoloCuentaCobrado(Verificador v) {
        PedidoManager m = conStock();

        pedir(m, "1", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        v.igual("un pedido pendiente no cuenta como facturado", 0.0, m.totalFacturado());

        String pagado = pedir(m, "2", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        m.cambiarEstado(pagado, "PAGADO");
        v.igual("un pedido pagado cuenta como facturado", 8500.0, m.totalFacturado());

        String cancelado = pedir(m, "3", "Mendoza", "VIN-MALBEC-750", 1, "RETIRO", false);
        m.cambiarEstado(cancelado, "CANCELADO");
        v.igual("un pedido cancelado no cuenta como facturado", 8500.0, m.totalFacturado());
    }

    /** Un reporte contable no puede salir en notacion cientifica. */
    static void importesConFormatoDeMoneda(Verificador v) {
        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 5000);
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
        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 500);
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

        List<Pedido> r1 = m.buscar(null, null, null, null, null);
        List<Pedido> r2 = m.buscar(null, null, null, null, null);
        StringBuilder o1 = new StringBuilder();
        StringBuilder o2 = new StringBuilder();
        for (Pedido p : r1) o1.append(p.numero).append(" ");
        for (Pedido p : r2) o2.append(p.numero).append(" ");
        v.igual("dos busquedas identicas devuelven el mismo orden",
                o1.toString(), o2.toString());
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
