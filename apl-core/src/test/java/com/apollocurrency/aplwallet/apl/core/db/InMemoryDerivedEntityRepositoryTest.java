/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class InMemoryDerivedEntityRepositoryTest {
    private DerivedIdEntity die1 = new DerivedIdEntity(null, 102, 1);
    private DerivedIdEntity die2 = new DerivedIdEntity(null, 103, 2);
    private DerivedIdEntity die3 = new DerivedIdEntity(null, 99, 3);

    private InMemoryDerivedEntityRepository<DerivedIdEntity> repository = new InMemoryDerivedEntityRepository<DerivedIdEntity>(new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(DerivedIdEntity derivedIdEntity) {
            return new LongKey(derivedIdEntity.getId());
        }
    });

    @BeforeEach
    void setUp() {
        putTestData();
    }

    private void putTestData() {
        repository.putAll(List.of(
                die1,
                die2,
                die3));
    }

    @Test
    void testGetAllEntities() {
        assertNothingChanged();
    }

    @Test
    void testClear() {
        repository.clear();

        Map<DbKey, DerivedIdEntity> allEntities = repository.getAllEntities();
        assertEquals(0, allEntities.size());
    }

    @Test
    void testRollback() {
        repository.rollback(99);

        Map<DbKey, DerivedIdEntity> allEntities = repository.getAllEntities();
        assertEquals(Map.of(new LongKey(die3.getId()), die3), allEntities);
    }

    @Test
    void testRollbackToZeroHeight() {
        repository.rollback(0);
        Map<DbKey, DerivedIdEntity> allEntities = repository.getAllEntities();
        assertEquals(0, allEntities.size());
    }

    @Test
    void testRollbackNothing() {
        repository.rollback(103);
        assertNothingChanged();
    }

    @Test
    void testGet() {
        DerivedIdEntity derivedIdEntity = repository.get(new LongKey(1L));
        assertSame(die1, derivedIdEntity);
        assertEquals(die1, derivedIdEntity);
    }

    @Test
    void testGetCopy() {
        DerivedIdEntity derivedIdEntity = repository.getCopy(new LongKey(2L));
        assertNotSame(die2, derivedIdEntity);
        assertEquals(die2, derivedIdEntity);
    }

    @Test
    void testGetCopyForEntityWhichNotExists() {
        DerivedIdEntity copy = repository.getCopy(new LongKey(5L));
        assertNull(copy, "Entity with id = 5 should not exist");
    }


    @Test
    void testInsertEntityWhichAlreadyExistsWithSameHeight() {
        DerivedIdEntity die = new DerivedIdEntity(null, 99, 3L);
        repository.insert(die);

        Map<LongKey, DerivedIdEntity> expected = Map.of(new LongKey(die1.getId()), die1, new LongKey(die2.getId()), die2, new LongKey(die3.getId()), die);
        Map<DbKey, DerivedIdEntity> actual = repository.getAllEntities();
        assertEquals(expected, actual);
    }

    @Test
    void testInsertEntityWhichAlreadyExistsWithAnotherHeight() {
        DerivedIdEntity die = new DerivedIdEntity(null, 104, 2L);
        assertThrows(IllegalArgumentException.class, () -> repository.insert(die));

    }

    @Test
    void testInsertNewEntity() {
        DerivedIdEntity die = new DerivedIdEntity(null, 103, 4L);
        repository.insert(die);

        Map<LongKey, DerivedIdEntity> expected = Map.of(new LongKey(die1.getId()), die1, new LongKey(die2.getId()), die2, new LongKey(die3.getId()), die3, new LongKey(4L), die);
        Map<DbKey, DerivedIdEntity> actual = repository.getAllEntities();
        assertEquals(expected, actual);
    }


    private void assertNothingChanged() {
        Map<DbKey, DerivedIdEntity> allEntities = repository.getAllEntities();
        Map<LongKey, DerivedIdEntity> expected = Map.of(new LongKey(die1.getId()), die1, new LongKey(die2.getId()), die2, new LongKey(die3.getId()), die3);
        assertEquals(expected, allEntities);
    }
}