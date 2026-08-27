package com.felipelopes.cryptrade

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * Prova que V1__init.sql / V2__ledger.sql batem com as entidades JPA contra Postgres real.
 * O teste padrão (H2, create-drop, flyway desligado) nunca exercita a migração de verdade.
 */
@SpringBootTest
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("it")
class FlywayPostgresIntegrationTest {

    @Test
    fun `flyway migrations match entities on real postgres`() {
        // contexto sobe = migracao rodou e ddl-auto=validate nao reprovou o schema
    }
}
