package com.academia.banco;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

@DisplayName("MP-3 · Verificar interacciones: qué se llamó, cuántas veces y en qué orden")
class InteraccionesTest {

    private RepositorioCuentas repositorio;
    private ServicioAntifraude antifraude;
    private Notificador notificador;
    private CajeroAutomatico cajero;
    private CuentaBancaria cuenta;

    @BeforeEach
    void preparar() {
        repositorio = mock(RepositorioCuentas.class);
        antifraude = mock(ServicioAntifraude.class);
        notificador = mock(Notificador.class);
        cajero = new CajeroAutomatico(repositorio, antifraude, notificador, Clock.systemDefaultZone());
        
        cuenta = new CuentaBancaria("Ana");
        cuenta.depositar(new BigDecimal("2000.00"));

        // Stubs por defecto para todas las pruebas
        when(repositorio.buscar("001")).thenReturn(Optional.of(cuenta));
        when(repositorio.totalRetiradoEn(eq("001"), any(LocalDate.class))).thenReturn(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("un retiro guarda la cuenta una vez y manda un SMS a esa cuenta")
    void retiroGuardaYMandaSms() {
        cajero.retirar("001", new BigDecimal("500.00"));

        verify(repositorio).guardar("001", cuenta); // Verifica que se llamó exactamente 1 vez
        verify(notificador).enviarSms(eq("001"), anyString());
    }

    @Test
    @DisplayName("el SMS dice el monto y el saldo que queda")
    void smsContenidoCorrecto() {
        cajero.retirar("001", new BigDecimal("500.00"));

        ArgumentCaptor<String> mensajeCapturado = ArgumentCaptor.forClass(String.class);
        verify(notificador).enviarSms(eq("001"), mensajeCapturado.capture());

        assertEquals("Retiro de $500.00. Saldo: $1500.00", mensajeCapturado.getValue());
    }

    @Test
    @DisplayName("con una cuenta que no existe, ni antifraude ni SMS")
    void cuentaInexistente() {
        when(repositorio.buscar("999")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> {
            cajero.retirar("999", new BigDecimal("500.00"));
        });

        verifyNoInteractions(antifraude, notificador); // A estos mocks no se les llamó nada
    }

    @Test
    @DisplayName("una operación sospechosa no cambia la cuenta, no la guarda y no manda SMS")
    void operacionSospechosa() {
        when(antifraude.esSospechosa("001", new BigDecimal("500.00"))).thenReturn(true);

        assertThrows(OperacionRechazadaException.class, () -> {
            cajero.retirar("001", new BigDecimal("500.00"));
        });

        assertAll(
            () -> assertEquals(new BigDecimal("2000.00"), cajero.consultarSaldo("001")),
            () -> verify(repositorio, never()).guardar(any(), any()),
            () -> verify(notificador, never()).enviarSms(anyString(), anyString())
        );
    }

    @Test
    @DisplayName("el orden: antifraude, después guardar, después SMS")
    void ordenDeInteracciones() {
        cajero.retirar("001", new BigDecimal("500.00"));

        // Se declara InOrder con todos los mocks cuyo orden se quiere verificar
        InOrder orden = inOrder(antifraude, repositorio, notificador);
        
        orden.verify(antifraude).esSospechosa(eq("001"), eq(new BigDecimal("500.00")));
        orden.verify(repositorio).guardar("001", cuenta);
        orden.verify(notificador).enviarSms(eq("001"), anyString());
    }

    @Test
    @DisplayName("si el SMS falla, el retiro ya se hizo y no se deshace")
    void smsFallaNoDeshaceRetiro() {
        // Configuramos para que falle al momento de enviar el SMS
        doThrow(new ServicioNoDisponibleException("SMS"))
                .when(notificador).enviarSms(anyString(), anyString());

        assertDoesNotThrow(() -> {
            cajero.retirar("001", new BigDecimal("500.00"));
        });

        assertAll(
            () -> assertEquals(new BigDecimal("1500.00"), cajero.consultarSaldo("001")),
            () -> verify(repositorio).guardar("001", cuenta)
        );
    }

    @Test
    @DisplayName("si la cuenta no tiene saldo, no se guarda ni se manda SMS")
    void sinSaldo() {
        assertThrows(SaldoInsuficienteException.class, () -> {
            cajero.retirar("001", new BigDecimal("3000.00"));
        });

        verify(repositorio, never()).guardar(any(), any());
        verify(notificador, never()).enviarSms(anyString(), anyString());
    }
}