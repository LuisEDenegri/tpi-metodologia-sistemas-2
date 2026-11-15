package pedidos;

import java.util.Arrays;

/**
 * Modalidad de entrega.
 *
 * REFACTORING: Replace Type Code with Enum.
 *
 * Antes era un String comparado con equals("RETIRO"). Eso significaba que
 * "retiro" en minuscula se cobraba como envio a domicilio, y que un valor
 * inexistente como "TELETRANSPORTE" entraba al sistema sin resistencia.
 */
public enum TipoEnvio {

    RETIRO,
    ENVIO;

    /**
     * Interpreta el texto ingresado. Tolera minusculas y espacios alrededor:
     * esos datos vienen de formularios y la diferencia no deberia costarle
     * plata a un cliente.
     *
     * @return el tipo de envio, o null si el texto no corresponde a ninguno.
     */
    public static TipoEnvio desdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(t -> t.name().equals(limpio))
                .findFirst()
                .orElse(null);
    }
}
