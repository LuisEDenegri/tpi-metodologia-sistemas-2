package pedidos;

import java.util.List;

/**
 * Demostracion del sistema funcionando. Datos sinteticos.
 *
 * Comparar este archivo entre estado-inicial y estado-refactorizado muestra el
 * efecto del refactoring en el punto de uso:
 *
 *   ANTES:  m.registrarPedido("30111222", "Maria Fernandez", "maria@ejemplo.com",
 *               "2615550001", "San Martin 100", "Mendoza", "Mendoza", "5500",
 *               "VIN-MALBEC-750", 2, "ENVIO", "Regalo, envolver", false);
 *
 *           Trece valores sueltos. Nueve Strings consecutivos. Para saber si
 *           "Mendoza" es la ciudad o la provincia hay que ir a buscar la firma
 *           y contar las comas.
 *
 *   AHORA:  cada dato entra por un objeto que dice que es, y el compilador
 *           rechaza lo que antes pasaba sin una advertencia.
 */
public class Main {

    public static void main(String[] args) {
        PedidoManager m = new PedidoManager();

        m.ingresarStock("VIN-MALBEC-750", 50);
        m.ingresarStock("ACE-OLIVA-500", 30);
        m.ingresarStock("DUL-MEMBRILLO-400", 12);
        m.ingresarStock("FRU-NUEZ-250", 40);

        System.out.println("=== REGISTRO DE PEDIDOS ===");

        Direccion casaDeMaria = new Direccion("San Martin 100", "Mendoza", "Mendoza", "5500");

        String p1 = m.registrar(new SolicitudDePedido("30111222", "Maria Fernandez",
                "maria@ejemplo.com", "2615550001", casaDeMaria,
                "VIN-MALBEC-750", 2, TipoEnvio.ENVIO, "Regalo, envolver", false));
        mostrar(m, "p1", p1);

        String p2 = m.registrar(new SolicitudDePedido("27333444", "Roberto Sanchez",
                "roberto@ejemplo.com", "3515550002",
                new Direccion("Colon 2000", "Cordoba", "Cordoba", "5000"),
                "ACE-OLIVA-500", 5, TipoEnvio.ENVIO, "", true));
        mostrar(m, "p2", p2);

        String p3 = m.registrar(new SolicitudDePedido("20555666", "Ana Gomez",
                "ana@ejemplo.com", "1155550003",
                new Direccion("Corrientes 500", "CABA", "CABA", "1043"),
                "DUL-MEMBRILLO-400", 4, TipoEnvio.ENVIO, "", false));
        mostrar(m, "p3", p3);

        String p4 = m.registrar(new SolicitudDePedido("30111222", "Maria Fernandez",
                "maria@ejemplo.com", "2615550001", casaDeMaria,
                "VIN-MALBEC-750", 4, TipoEnvio.RETIRO, "", false));
        mostrar(m, "p4", p4);

        String sinStock = m.registrar(new SolicitudDePedido("20999888", "Carlos Ruiz",
                "carlos@ejemplo.com", "2615550004",
                new Direccion("Belgrano 50", "Mendoza", "Mendoza", "5500"),
                "DUL-MEMBRILLO-400", 999, TipoEnvio.ENVIO, "", false));
        System.out.println("-> pedido sin stock devuelve: " + sinStock);
        System.out.println("   y NO deja el cliente registrado: "
                + (m.getCliente("20999888") == null));

        System.out.println();
        System.out.println("=== DEFECTO CORREGIDO: recargo por urgencia ===");
        mostrarComparacionDeUrgencia();

        System.out.println();
        System.out.println("=== CAMBIOS DE ESTADO ===");
        System.out.println("p1 a PAGADO: " + m.cambiarEstado(p1, EstadoPedido.PAGADO));
        System.out.println("p1 a PREPARACION: " + m.cambiarEstado(p1, EstadoPedido.PREPARACION));
        System.out.println("p1 a ENTREGADO (salteado): "
                + m.cambiarEstado(p1, EstadoPedido.ENTREGADO));
        System.out.println("p2 a CANCELADO: " + m.cambiarEstado(p2, EstadoPedido.CANCELADO));
        System.out.println("stock aceite tras cancelar: " + m.stockDe("ACE-OLIVA-500"));

        System.out.println();
        System.out.println("=== ASIGNACION ===");
        System.out.println("p1: " + m.asignarResponsable(p1, "Laura Diaz", "DEPOSITO"));

        System.out.println();
        System.out.println("=== BUSQUEDA ===");
        List<Pedido> urgentes = m.buscar(new FiltroDePedidos().soloUrgentes());
        System.out.println("urgentes: " + urgentes.size());
        List<Pedido> mendoza = m.buscar(new FiltroDePedidos().enProvincia("Mendoza"));
        System.out.println("de Mendoza: " + mendoza.size());

        System.out.println();
        System.out.println(m.reporte());
    }

    static void mostrar(PedidoManager m, String etiqueta, String numero) {
        if (numero == null) {
            System.out.println("-> " + etiqueta + " rechazado");
            return;
        }
        System.out.println("-> " + numero + " total $" + m.getPedido(numero).total());
    }

    /**
     * El defecto principal, lado a lado. Dos pedidos identicos salvo la
     * urgencia, por encima del umbral de envio bonificado.
     */
    static void mostrarComparacionDeUrgencia() {
        PedidoManager m = new PedidoManager(new NotificadorSilencioso());
        m.ingresarStock("VIN-MALBEC-750", 100);

        Direccion cordoba = new Direccion("Colon 2000", "Cordoba", "Cordoba", "5000");

        String normal = m.registrar(new SolicitudDePedido("1", "Normal", "n@e.com", "1",
                cordoba, "VIN-MALBEC-750", 4, TipoEnvio.ENVIO, "", false));
        String urgente = m.registrar(new SolicitudDePedido("2", "Urgente", "u@e.com", "1",
                cordoba, "VIN-MALBEC-750", 4, TipoEnvio.ENVIO, "", true));

        System.out.println("  Pedido de $34.000 a Cordoba (supera el umbral de envio gratis)");
        System.out.println("    normal  -> envio $" + m.getPedido(normal).envio()
                + ", total $" + m.getPedido(normal).total());
        System.out.println("    urgente -> envio $" + m.getPedido(urgente).envio()
                + ", total $" + m.getPedido(urgente).total());
        System.out.println("  Antes del refactoring ambos costaban lo mismo: el recargo por");
        System.out.println("  urgencia se perdia justo en los pedidos mas grandes.");
    }
}
