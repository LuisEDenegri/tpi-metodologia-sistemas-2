package sondas;

import pedidos.Cliente;
import pedidos.PedidoManager;

/**
 * Sonda de diagnostico. No forma parte del sistema entregable: existe para
 * DEMOSTRAR con ejecucion real cuales defectos existen y cuales no, antes de
 * tocar una sola linea del codigo de produccion.
 *
 * Regla de esta sonda: no afirma nada que no haya comparado. Cada verificacion
 * calcula el valor real, lo compara contra el esperado e imprime CONFIRMADO o
 * DESCARTADO segun el resultado. Un diagnostico que se escribe de antemano no
 * es un diagnostico, es una opinion.
 */
public class SondaDeDefectos {

    private static int confirmados = 0;
    private static int descartados = 0;

    public static void main(String[] args) {
        recargoUrgenteSeEvapora();
        cancelarDosVecesInflaStock();
        datosDelClienteQuedanViejos();
        dineroEnDoublePierdePrecision();
        estadoExpuestoPermiteCorrupcion();
        ordenDelReporteNoEsDeterminista();

        System.out.println();
        System.out.println("=========================================================");
        System.out.println("RESUMEN: " + confirmados + " defectos confirmados, "
                + descartados + " hipotesis descartadas");
        System.out.println("=========================================================");
    }

    static void titulo(String t) {
        System.out.println();
        System.out.println("=========================================================");
        System.out.println(t);
        System.out.println("=========================================================");
    }

    /** Compara y rotula segun el resultado real, no segun lo que se esperaba encontrar. */
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

    /**
     * Hipotesis: el recargo del 50% por urgente se pierde cuando el subtotal
     * supera el umbral de envio gratis, porque el umbral se evalua despues.
     */
    static void recargoUrgenteSeEvapora() {
        titulo("HIPOTESIS 1: el recargo por URGENTE se evapora sobre el umbral");

        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 100);

        String normal = m.registrarPedido("1", "Normal", "n@e.com", "1", "c", "ci",
                "Cordoba", "5000", "VIN-MALBEC-750", 4, "ENVIO", "", false);
        String urgente = m.registrarPedido("2", "Urgente", "u@e.com", "1", "c", "ci",
                "Cordoba", "5000", "VIN-MALBEC-750", 4, "ENVIO", "", true);

        double tNormal = m.getPedido(normal).total;
        double tUrgente = m.getPedido(urgente).total;

        System.out.println("  Caso A - 4 vinos a Cordoba, subtotal 34000 (SUPERA el umbral):");
        System.out.println("    normal  -> envio " + m.getPedido(normal).envio + ", total " + tNormal);
        System.out.println("    urgente -> envio " + m.getPedido(urgente).envio + ", total " + tUrgente);

        PedidoManager m2 = new PedidoManager();
        m2.cargarStock("VIN-MALBEC-750", 100);
        String n2 = m2.registrarPedido("1", "Normal", "n@e.com", "1", "c", "ci",
                "Cordoba", "5000", "VIN-MALBEC-750", 2, "ENVIO", "", false);
        String u2 = m2.registrarPedido("2", "Urgente", "u@e.com", "1", "c", "ci",
                "Cordoba", "5000", "VIN-MALBEC-750", 2, "ENVIO", "", true);

        System.out.println("  Caso B - 2 vinos a Cordoba, subtotal 17000 (NO supera):");
        System.out.println("    normal  -> envio " + m2.getPedido(n2).envio);
        System.out.println("    urgente -> envio " + m2.getPedido(u2).envio);

        boolean cobraEnB = m2.getPedido(u2).envio > m2.getPedido(n2).envio;
        boolean noCobraEnA = tUrgente == tNormal;

        verificar("el urgente no paga recargo cuando supera el umbral",
                noCobraEnA && cobraEnB,
                "Bajo el umbral el recargo se cobra (" + m2.getPedido(n2).envio + " -> "
                        + m2.getPedido(u2).envio + "), sobre el umbral se pierde. "
                        + "El servicio premium se regala justo en los pedidos mas grandes.");
    }

    /**
     * Hipotesis: cancelar un pedido ya cancelado repone el stock por segunda vez.
     */
    static void cancelarDosVecesInflaStock() {
        titulo("HIPOTESIS 2: cancelar dos veces repone stock dos veces");

        PedidoManager m = new PedidoManager();
        m.cargarStock("ACE-OLIVA-500", 10);
        int inicial = m.stockDe("ACE-OLIVA-500");

        String p = m.registrarPedido("1", "Cliente", "c@e.com", "1", "c", "ci",
                "Mendoza", "5500", "ACE-OLIVA-500", 5, "RETIRO", "", false);
        int trasPedir = m.stockDe("ACE-OLIVA-500");

        boolean primera = m.cambiarEstado(p, "CANCELADO");
        int trasUna = m.stockDe("ACE-OLIVA-500");

        boolean segunda = m.cambiarEstado(p, "CANCELADO");
        int trasDos = m.stockDe("ACE-OLIVA-500");

        System.out.println("  stock inicial " + inicial + " -> tras pedir 5: " + trasPedir
                + " -> tras cancelar: " + trasUna + " -> tras recancelar: " + trasDos);
        System.out.println("  primera cancelacion devolvio " + primera
                + ", segunda devolvio " + segunda);

        verificar("la segunda cancelacion vuelve a reponer stock",
                trasDos != inicial,
                trasDos == inicial
                        ? "El estado CANCELADO cae en el else final y devuelve false, asi que "
                          + "la reposicion no se repite. El codigo YA estaba protegido aca. "
                          + "Se deja asentado para no reportar un defecto inexistente."
                        : "Stock quedo en " + trasDos + " cuando deberia ser " + inicial);
    }

    /**
     * Hipotesis: cuando un cliente ya existente vuelve a pedir con datos
     * distintos, el sistema conserva los viejos.
     */
    static void datosDelClienteQuedanViejos() {
        titulo("HIPOTESIS 3: los datos del cliente nunca se actualizan");

        PedidoManager m = new PedidoManager();
        m.cargarStock("DUL-MEMBRILLO-400", 100);

        m.registrarPedido("30111222", "Maria Fernandez", "maria@viejo.com", "2615550001",
                "San Martin 100", "Mendoza", "Mendoza", "5500",
                "DUL-MEMBRILLO-400", 1, "ENVIO", "", false);

        System.out.println("  Maria se muda a Cordoba y cambia de mail. Pide de nuevo:");
        String p2 = m.registrarPedido("30111222", "Maria Fernandez", "maria@nuevo.com",
                "3515559999", "Colon 2000", "Cordoba", "Cordoba", "5000",
                "DUL-MEMBRILLO-400", 1, "ENVIO", "", false);

        Cliente guardado = m.clientes.get("30111222");
        double envioCobrado = m.getPedido(p2).envio;

        System.out.println("  envio cobrado en el pedido nuevo: " + envioCobrado
                + "  (tarifa de Cordoba, interior)");
        System.out.println("  provincia guardada del cliente:    " + guardado.provincia);
        System.out.println("  direccion guardada:                " + guardado.calle
                + ", " + guardado.ciudad);
        System.out.println("  mail al que se notifico:           " + guardado.email);

        boolean mailViejo = guardado.email.equals("maria@viejo.com");
        boolean domicilioViejo = guardado.provincia.equals("Mendoza");
        boolean cobroPorNueva = envioCobrado == 4500;

        verificar("los datos del cliente quedan congelados en el primer alta",
                mailViejo && domicilioViejo,
                "Mail y domicilio siguen siendo los del primer pedido. Ademas hay una "
                        + "INCOHERENCIA mas grave: el envio se cobro " + envioCobrado
                        + " usando la provincia NUEVA (parametro), pero el paquete se "
                        + "despacha al domicilio VIEJO (Mendoza). Se cobra un destino y se "
                        + "envia a otro. Cobro por provincia nueva: " + cobroPorNueva);
    }

    /** Hipotesis: usar double para dinero acumula error. */
    static void dineroEnDoublePierdePrecision() {
        titulo("HIPOTESIS 4: dinero representado en double");

        double suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += 1234.10;
        }
        double esperado = 12341.0;

        System.out.println("  0.1 + 0.2            = " + (0.1 + 0.2));
        System.out.println("  1234.10 x 10 sumado  = " + suma);
        System.out.println("  esperado             = " + esperado);

        verificar("la aritmetica de dinero en double arrastra error",
                suma != esperado,
                "Diferencia de " + Math.abs(suma - esperado) + ". Sobre miles de pedidos "
                        + "la facturacion acumulada no cierra contra contabilidad.");
    }

    /** Hipotesis: los campos publicos permiten corromper el estado desde afuera. */
    static void estadoExpuestoPermiteCorrupcion() {
        titulo("HIPOTESIS 5: estado interno publico y mutable");

        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 10);
        String p = m.registrarPedido("1", "Cliente", "c@e.com", "1", "c", "ci",
                "Mendoza", "5500", "VIN-MALBEC-750", 1, "RETIRO", "", false);

        m.stock.put("VIN-MALBEC-750", -999);
        m.getPedido(p).total = 0;
        m.getPedido(p).estado = "INVENTADO";

        int stockCorrupto = m.stockDe("VIN-MALBEC-750");
        String estadoCorrupto = m.getPedido(p).estado;

        System.out.println("  stock forzado a negativo:  " + stockCorrupto);
        System.out.println("  total forzado a cero:      " + m.getPedido(p).total);
        System.out.println("  estado fuera del ciclo:    " + estadoCorrupto);

        verificar("codigo externo puede violar las invariantes del dominio",
                stockCorrupto < 0 && estadoCorrupto.equals("INVENTADO"),
                "Ningun metodo valido nada. Un stock negativo y un estado que no existe "
                        + "en el ciclo de vida fueron aceptados sin resistencia.");
    }

    /**
     * Hipotesis: el reporte recorre un HashMap, por lo que el orden de las
     * filas no esta garantizado y no es el de creacion.
     */
    static void ordenDelReporteNoEsDeterminista() {
        titulo("HIPOTESIS 6: el orden del reporte no sigue el de creacion");

        PedidoManager m = new PedidoManager();
        m.cargarStock("VIN-MALBEC-750", 500);
        for (int i = 1; i <= 6; i++) {
            m.registrarPedido("doc" + i, "Cliente " + i, "c" + i + "@e.com", "1",
                    "c", "ci", "Mendoza", "5500", "VIN-MALBEC-750", 1, "RETIRO", "", false);
        }

        String reporte = m.reporte();
        StringBuilder orden = new StringBuilder();
        for (String linea : reporte.split("\n")) {
            if (linea.startsWith("PED-")) {
                orden.append(linea.substring(0, linea.indexOf(' '))).append(" ");
            }
        }
        String obtenido = orden.toString().trim();
        String esperado = "PED-1 PED-2 PED-3 PED-4 PED-5 PED-6";

        System.out.println("  orden de creacion: " + esperado);
        System.out.println("  orden del reporte: " + obtenido);

        verificar("el reporte no respeta el orden de creacion",
                !obtenido.equals(esperado),
                "Se recorre keySet() de un HashMap. El orden depende del hash de la clave, "
                        + "no del alta. Un listado administrativo que cambia de orden sin "
                        + "motivo aparente es imposible de auditar.");
    }
}
