package com.academia.banco;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MP-6 · El límite diario con un reloj fijo (Clock.fixed, no un mock)")
class RelojTest {

    private static final ZoneId CDMX = ZoneId.of("America/Mexico_City");

    private RepositorioCuentas repositorio;
    private CuentaBancaria cuenta;

    @BeforeEach
    void preparar() {
        repositorio = mock(RepositorioCuentas.class);
        cuenta = new CuentaBancaria("Ana");
        cuenta.depositar(new BigDecimal("2000.00"));
        when(repositorio.buscar("001")).thenReturn(Optional.of(cuenta));
    }

    private CajeroAutomatico cajeroConReloj(String instante) {
        Clock reloj = Clock.fixed(Instant.parse(instante), CDMX);
        return new CajeroAutomatico(repositorio, mock(ServicioAntifraude.class), mock(Notificador.class), reloj);
    }

    @Test
    @DisplayName("el límite usa lo retirado en la fecha del reloj del cajero")
    void usaLaFechaDelReloj() {
        CajeroAutomatico cajero = cajeroConReloj("2026-10-01T18:00:00Z");      // 1-oct 12:00 en CDMX
        when(repositorio.totalRetiradoEn("001", LocalDate.of(2026, 10, 1))).thenReturn(new BigDecimal("7600.00"));

        assertThrows(LimiteDiarioExcedidoException.class, () -> cajero.retirar("001", new BigDecimal("500.00")));
    }

    @Test
    @DisplayName("a las 21:30 de CDMX todavía es el mismo día (aunque en UTC ya sea el siguiente)")
    void mismoDiaEnCDMX() {
        // En UTC es el 2 de octubre a las 03:30 AM, pero en CDMX (UTC-6) sigue siendo el 1 de octubre a las 21:30 PM.
        CajeroAutomatico cajero = cajeroConReloj("2026-10-02T03:30:00Z");      
        
        // El cajero consultará el total retirado el 1 de octubre.
        when(repositorio.totalRetiradoEn("001", LocalDate.of(2026, 10, 1))).thenReturn(new BigDecimal("7600.00"));

        // Al sumar 500 al acumulado de 7600 superará el límite de 8000.
        assertThrows(LimiteDiarioExcedidoException.class, () -> cajero.retirar("001", new BigDecimal("500.00")));
    }

    @Test
    @DisplayName("al día siguiente el límite empieza de cero")
    void alDiaSiguienteLimiteEnCero() {
        // En UTC es el 2 de octubre a las 18:00 PM, en CDMX es el 2 de octubre a las 12:00 PM.
        CajeroAutomatico cajero = cajeroConReloj("2026-10-02T18:00:00Z");      
        
        // El cajero ahora consultará el total retirado el 2 de octubre, porque el reloj ya marca el día siguiente.
        when(repositorio.totalRetiradoEn("001", LocalDate.of(2026, 10, 2))).thenReturn(new BigDecimal("0.00"));

        // Retira 500 sin problemas.
        cajero.retirar("001", new BigDecimal("500.00"));

        // Comprobamos que el saldo bajó correctamente de 2000 a 1500.
        assertEquals(new BigDecimal("1500.00"), cajero.consultarSaldo("001"));
    }
}