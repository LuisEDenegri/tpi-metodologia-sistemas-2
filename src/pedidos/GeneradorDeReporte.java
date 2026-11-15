package pedidos;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Arma el reporte de pedidos para administracion.
 *
 * REFACTORING: Extract Class (Fowler).
 *
 * El metodo reporte() original tenia 55 lineas y seis contadores declarados a
 * mano, uno por estado, con una cadena de if/else que los incrementaba. Agregar
 * un estado obligaba a tocar cuatro lugares distintos del mismo metodo.
 *
 * Separarlo ademas cumple el principio de responsabilidad unica: presentar no
 * es lo mismo que gestionar. El gestor de pedidos no deberia saber como se
 * formatea un listado.
 *
 * CORRECCION: los importes se formatean con Dinero. Antes se concatenaba
 * "$" + double, que delega en Double.toString() y pasa a notacion cientifica
 * desde 10^7: el reporte contable mostraba $1.7E7 en lugar de $17.000.000,00.
 */
public class GeneradorDeReporte {

    private static final String SEPARADOR = " | ";

    public String generar(Collection<Pedido> pedidos, Map<String, Cliente> clientes) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE PEDIDOS ===\n");

        Map<EstadoPedido, Integer> conteo = new EnumMap<>(EstadoPedido.class);
        for (EstadoPedido e : EstadoPedido.values()) {
            conteo.put(e, 0);
        }

        Dinero facturado = Dinero.CERO;

        for (Pedido p : pedidos) {
            conteo.merge(p.estado(), 1, Integer::sum);
            if (p.cuentaComoFacturado()) {
                facturado = facturado.mas(p.total());
            }
            sb.append(lineaDe(p, clientes.get(p.documentoCliente()))).append("\n");
        }

        sb.append("--- Pendientes: ").append(conteo.get(EstadoPedido.PENDIENTE)).append("\n");
        sb.append("--- Pagados: ").append(conteo.get(EstadoPedido.PAGADO)).append("\n");
        sb.append("--- En preparacion: ").append(conteo.get(EstadoPedido.PREPARACION)).append("\n");
        sb.append("--- Despachados: ").append(conteo.get(EstadoPedido.DESPACHADO)).append("\n");
        sb.append("--- Entregados: ").append(conteo.get(EstadoPedido.ENTREGADO)).append("\n");
        sb.append("--- Cancelados: ").append(conteo.get(EstadoPedido.CANCELADO)).append("\n");
        sb.append("--- Facturado: $").append(facturado).append("\n");

        return sb.toString();
    }

    /** REFACTORING: Extract Method. Armar una fila no es contar totales. */
    private String lineaDe(Pedido p, Cliente cliente) {
        String nombre = cliente == null ? "" : cliente.nombre();
        StringBuilder linea = new StringBuilder();
        linea.append(p.numero()).append(SEPARADOR)
                .append(nombre).append(SEPARADOR)
                .append(p.codigoProducto()).append(" x").append(p.cantidad()).append(SEPARADOR)
                .append(p.estado()).append(SEPARADOR)
                .append("$").append(p.total());
        if (p.esUrgente()) {
            linea.append(SEPARADOR).append("URGENTE");
        }
        return linea.toString();
    }
}
