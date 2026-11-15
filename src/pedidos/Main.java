package pedidos;

import java.util.List;

/**
 * Demostracion del sistema funcionando. Datos sinteticos.
 */
public class Main {

    public static void main(String[] args) {
        PedidoManager m = new PedidoManager();

        m.cargarStock("VIN-MALBEC-750", 50);
        m.cargarStock("ACE-OLIVA-500", 30);
        m.cargarStock("DUL-MEMBRILLO-400", 12);
        m.cargarStock("FRU-NUEZ-250", 40);

        System.out.println("=== REGISTRO DE PEDIDOS ===");

        String p1 = m.registrarPedido("30111222", "Maria Fernandez", "maria@ejemplo.com",
                "2615550001", "San Martin 100", "Mendoza", "Mendoza", "5500",
                "VIN-MALBEC-750", 2, "ENVIO", "Regalo, envolver", false);
        System.out.println("-> " + p1 + " total $" + m.getPedido(p1).total);

        String p2 = m.registrarPedido("27333444", "Roberto Sanchez", "roberto@ejemplo.com",
                "3515550002", "Colon 2000", "Cordoba", "Cordoba", "5000",
                "ACE-OLIVA-500", 5, "ENVIO", "", true);
        System.out.println("-> " + p2 + " total $" + m.getPedido(p2).total);

        String p3 = m.registrarPedido("20555666", "Ana Gomez", "ana@ejemplo.com",
                "1155550003", "Corrientes 500", "CABA", "CABA", "1043",
                "DUL-MEMBRILLO-400", 4, "ENVIO", "", false);
        System.out.println("-> " + p3 + " total $" + m.getPedido(p3).total);

        String p4 = m.registrarPedido("30111222", "Maria Fernandez", "maria@ejemplo.com",
                "2615550001", "San Martin 100", "Mendoza", "Mendoza", "5500",
                "VIN-MALBEC-750", 4, "RETIRO", "", false);
        System.out.println("-> " + p4 + " total $" + m.getPedido(p4).total);

        String sinStock = m.registrarPedido("20999888", "Carlos Ruiz", "carlos@ejemplo.com",
                "2615550004", "Belgrano 50", "Mendoza", "Mendoza", "5500",
                "DUL-MEMBRILLO-400", 999, "ENVIO", "", false);
        System.out.println("-> pedido sin stock devuelve: " + sinStock);

        System.out.println();
        System.out.println("=== CAMBIOS DE ESTADO ===");
        System.out.println("p1 a PAGADO: " + m.cambiarEstado(p1, "PAGADO"));
        System.out.println("p1 a PREPARACION: " + m.cambiarEstado(p1, "PREPARACION"));
        System.out.println("p1 a ENTREGADO (salteado): " + m.cambiarEstado(p1, "ENTREGADO"));
        System.out.println("p2 a CANCELADO: " + m.cambiarEstado(p2, "CANCELADO"));
        System.out.println("stock aceite tras cancelar: " + m.stockDe("ACE-OLIVA-500"));

        System.out.println();
        System.out.println("=== ASIGNACION ===");
        System.out.println("p1: " + m.asignarResponsable(p1, "Laura Diaz", "DEPOSITO"));

        System.out.println();
        System.out.println("=== BUSQUEDA ===");
        List<Pedido> urgentes = m.buscar(null, null, null, true, null);
        System.out.println("urgentes: " + urgentes.size());
        List<Pedido> mendoza = m.buscar(null, "Mendoza", null, null, null);
        System.out.println("de Mendoza: " + mendoza.size());

        System.out.println();
        System.out.println(m.reporte());
    }
}
