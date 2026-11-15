package pedidos;

/**
 * Registro de eventos para el panel de administracion.
 */
public class AnalyticsService {

    public void registrar(String evento, String referencia) {
        System.out.println("[ANALYTICS] " + evento + " :: " + referencia);
    }
}
