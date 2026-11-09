package br.com.hippo.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import br.com.hippo.data.local.db.HippoDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Simula um aparelho que já tinha o app na versão 1 do banco (só tarefas) e recebe a
 * atualização: a migração precisa criar as tabelas novas sem perder as tarefas.
 */
class DatabaseMigrationTest {

    @Test
    fun `migracao da versao 1 para a atual mantem as tarefas`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // Esquema exatamente como era na versão 1.
        driver.execute(
            null,
            """
            CREATE TABLE TaskEntity (
                id TEXT NOT NULL PRIMARY KEY,
                owner_id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                priority TEXT NOT NULL,
                due_date TEXT,
                due_time TEXT,
                reminder TEXT NOT NULL,
                is_done INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            0,
        )
        driver.execute(
            null,
            "INSERT INTO TaskEntity VALUES ('t1', 'maria', 'Antiga', '', 'HIGH', NULL, NULL, 'NONE', 0)",
            0,
        )

        HippoDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = HippoDatabase.Schema.version)
        val database = HippoDatabase(driver)

        assertEquals("Antiga", database.taskEntityQueries.selectByOwner("maria").executeAsOne().title)
        database.voiceInteractionEntityQueries.insert(
            "maria", "2026-11-09T12:00:00Z", "local", "teste", null, null, null, null, "NOT_UNDERSTOOD", 100, 5,
        )
        assertEquals(1, database.voiceInteractionEntityQueries.selectByOwner("maria").executeAsList().size)
        driver.close()
    }
}
