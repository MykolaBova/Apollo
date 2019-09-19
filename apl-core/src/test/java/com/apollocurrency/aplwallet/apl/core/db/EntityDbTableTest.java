/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
public abstract class EntityDbTableTest<T extends DerivedEntity> extends BasicDbTableTest<T> {

    private DbKey THROWING_DB_KEY = createThrowingKey();
    private Comparator<T> DB_ID_HEIGHT_COMPARATOR = Comparator.comparing(T::getHeight).thenComparing(T::getDbId).reversed();
    private String DB_ID_HEIGHT_SORT = " ORDER BY height DESC, db_id DESC";

    private DbKey createThrowingKey() {
        DbKey throwingKey = mock(DbKey.class);
        try {
            doThrow(SQLException.class).when(throwingKey).setPK(any(PreparedStatement.class));
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return throwingKey;
    }

    public EntityDbTableTest(Class<T> clazz) {
        super(clazz);
    }

    EntityDbTable<T> table;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        table = (EntityDbTable<T>) getDerivedDbTable();
        getBlockchain().setLastBlock(new BlockTestData().LAST_BLOCK);
    }

    @Test
    public void testGetByDbKey() {
        List<T> all = getAllLatest();
        T expected = all.get(1);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        T actual = table.get(dbKey);
        assertEquals(expected, actual);
    }

    @Test
    public void testByUnknownDbKey() {
        T unknownValue = table.get(table.getDbKeyFactory().newKey(valueToInsert()));

        assertNull(unknownValue, "value with unknown db key should be null");
    }

    @Test
    public void testGetFromCache() {
        List<T> all = getAllLatest();
        T expected = all.get(2);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        DbUtils.inTransaction(extension, (con) -> {
            T actual = table.get(dbKey, true);
            assertEquals(expected, actual);
        });
        T actual = table.get(dbKey, true);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetWithoutCache() {
        List<T> all = getAllLatest();
        T expected = all.get(0);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        DbUtils.inTransaction(extension, (con) -> {
            T actual = table.get(dbKey, false);
            assertEquals(expected, actual);
        });
        T actual = table.get(dbKey, false);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetFromCacheWithoutTransaction() {
        List<T> all = getAllLatest();
        T expected = all.get(1);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        T actual = table.get(dbKey, true);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetWithSqlException() throws SQLException {

        assertThrows(RuntimeException.class, () -> table.get(THROWING_DB_KEY));
    }

    @Test
    public void testGetByHeight() {

        if (table.isMultiversion()) {
            Block mock = mock(Block.class);
            doReturn(Integer.MAX_VALUE).when(mock).getHeight();
            getBlockchain().setLastBlock(mock);
            Map.Entry<DbKey, List<T>> entries = getEntryWithListOfSize(getAll(), table.getDbKeyFactory(), 3, true);
            List<T> sorted = sortByHeightDesc(entries.getValue());
            T latest = sorted.get(0);
            T notLatest = sorted.get(1);
            T first = sorted.get(2);

            assertNotEquals(latest.getHeight(), notLatest.getHeight());
            assertNotEquals(notLatest.getHeight(), first.getHeight());

            T actual = table.get(entries.getKey(), latest.getHeight() + 1);
            assertEquals(latest, actual);

            actual = table.get(entries.getKey(), latest.getHeight());
            assertEquals(latest, actual);

            actual = table.get(entries.getKey(), latest.getHeight() - 1);
            assertEquals(notLatest, actual);

            actual = table.get(entries.getKey(), notLatest.getHeight());
            assertEquals(notLatest, actual);

            actual = table.get(entries.getKey(), notLatest.getHeight() - 1);
            assertEquals(first, actual);

            actual = table.get(entries.getKey(), first.getHeight());
            assertEquals(first, actual);

            T deleted = getDeletedMultiversionRecord().get(0);

            actual = table.get(table.getDbKeyFactory().newKey(deleted), deleted.getHeight());

            assertNull(actual);

            entries = getEntryWithListOfSize(getAll(), table.getDbKeyFactory(), 1, true);

            T expected = entries.getValue().get(0);

            actual = table.get(entries.getKey(), expected.getHeight());
            assertEquals(expected, actual);

            actual = table.get(entries.getKey(), expected.getHeight() + 1);
            assertEquals(expected, actual);
        } else {
            T expected = getAll().get(1);
            T actual = table.get(table.getDbKeyFactory().newKey(expected), expected.getHeight());
            assertEquals(expected, actual);

            actual = table.get(table.getDbKeyFactory().newKey(expected), expected.getHeight() + 1);
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testGetByHeightForUnknownDbKey() {
        T unknownEntity = table.get(table.getDbKeyFactory().newKey(valueToInsert()), Integer.MAX_VALUE);
        assertNull(unknownEntity, "Entity with unknown db key should not exist");
    }

    @Test
    public void testGetByHeightWithException() {
        assertThrows(RuntimeException.class, () -> table.get(THROWING_DB_KEY, Integer.MAX_VALUE));
    }

    @Test
    public void testGetByDbClause() {
        T expected;
        if (table.isMultiversion()) {

            expected = getAllLatest().get(0);
        } else {
            expected = getAll().get(2);
        }
        T actual = table.getBy(new DbClause.IntClause("height", DbClause.Op.EQ, expected.getHeight()).and(new DbClause.LongClause("db_id", DbClause.Op.EQ, expected.getDbId())));
        assertEquals(expected, actual);
    }

    @Test
    public void testGetByDbClauseUsingIncorrectDbId() {
        T uknownEntity = table.getBy(new DbClause.LongClause("db_id", DbClause.Op.EQ, Long.MAX_VALUE));
        assertNull(uknownEntity, "Entity with unknown db_id should not exist");
    }

    @Test
    public void testGetByDbClauseWithHeight() {
        T expected;
        if (table.isMultiversion()) {

            expected = getAllLatest().get(1);
        } else {
            expected = getAll().get(2);
        }
        T actual = table.getBy((new DbClause.LongClause("db_id", DbClause.Op.EQ, expected.getDbId())), expected.getHeight());
        assertEquals(expected, actual);
    }

    @Test
    public void testGetByDbClauseWithHeightUsingIncorrectDbId() {
        long incorrectDbId = getIncorrectDbId();

        T uknownEntity = table.getBy(new DbClause.LongClause("db_id", DbClause.Op.EQ, incorrectDbId));
        assertNull(uknownEntity, "Entity with unknown db_id should not exist");
    }

    public long getIncorrectDbId() {
        long incorrectDbId;
        if (table.isMultiversion()) {
            incorrectDbId = getDeletedMultiversionRecord().get(0).getDbId();
        } else {
            incorrectDbId = Long.MAX_VALUE;
        }
        return incorrectDbId;
    }

//    public List<T> getAllLatest() {
//        return sortByHeightDesc(groupByDbKey(table.getDbKeyFactory()).values().stream().map(l -> sortByHeightDesc(l).get(0)).collect(Collectors.toList()));
//    }

    @Test
    public void testGetManyByEmptyClause() {
        List<T> all = CollectionUtil.toList(table.getManyBy(DbClause.EMPTY_CLAUSE, 0, Integer.MAX_VALUE));
        List<T> expected = getAllLatest().stream().sorted(getDefaultComparator()).collect(Collectors.toList());
        assertEquals(expected, all);
    }


    @Test
    public void testGetManyByEmptyClauseWithOffset() {
        List<T> all = CollectionUtil.toList(table.getManyBy(DbClause.EMPTY_CLAUSE, 2, Integer.MAX_VALUE));
        List<T> allExpectedData = getAllLatest();
        List<T> expected = allExpectedData.stream().sorted(getDefaultComparator()).skip(2).collect(Collectors.toList());
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByEmptyClauseWithLimit() {
        List<T> allExpectedData = getAllLatest();
        List<T> all = CollectionUtil.toList(table.getManyBy(DbClause.EMPTY_CLAUSE, 0, allExpectedData.size() - 2));
        List<T> expected = allExpectedData.stream().sorted(getDefaultComparator()).limit(allExpectedData.size() - 1).collect(Collectors.toList());
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByEmptyClauseWithLimitAndOffset() {
        List<T> allExpectedData = getAllLatest();
        List<T> all = CollectionUtil.toList(table.getManyBy(DbClause.EMPTY_CLAUSE, 1, allExpectedData.size() - 2));
        List<T> expected = allExpectedData.stream().sorted(getDefaultComparator()).skip(1).limit(allExpectedData.size() - 2).collect(Collectors.toList());
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByIncorrectClause() {
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.LongClause("db_id", getIncorrectDbId()), 0, Integer.MAX_VALUE));
        assertEquals(0, all.size());
    }

    @Test
    public void testGetManyByHeightAllClause() {
        List<T> expected = getAllLatest().stream().sorted(getDefaultComparator()).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, 0), 0, expected.size()));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightWithUpperBound() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int height = heights.get(1);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() < height).sorted(getDefaultComparator()).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, 0).and(new DbClause.IntClause("height", DbClause.Op.LT, height)), 0, expected.size() - 1));
        assertEquals(expected, all);
    }


    @Test
    public void testGetManyByHeightInRange() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int upperHeight = heights.get(1);
        int lowerHeight = heights.get(2);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() < upperHeight && t.getHeight() >= lowerHeight).sorted(getDefaultComparator()).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LT, upperHeight)), 0, expected.size() - 1));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightInRangeExclusive() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int upperHeight = heights.get(0);
        int lowerHeight = heights.get(2);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() < upperHeight && t.getHeight() > lowerHeight).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GT, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LT, upperHeight)), 0, expected.size() - 1));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightWithNotDefinedHeight() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);

        int notExpectedHeight = heights.get(0);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() != notExpectedHeight).sorted(getDefaultComparator()).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.NE, notExpectedHeight), 0, expected.size() - 1));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightWithDefinedHeight() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);

        int expectedHeight = heights.get(1);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() == expectedHeight).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.EQ, expectedHeight), 0, expected.size() - 1));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightWithInRangeInclusive() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int upperHeight = heights.get(0);
        int lowerHeight = heights.get(2);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() > lowerHeight && t.getHeight() < upperHeight).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GT, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LT, upperHeight)), 0, expected.size() - 1));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightInRangeWithPagination() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int upperHeight = heights.get(1);
        int lowerHeight = heights.get(2);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() <= upperHeight && t.getHeight() >= lowerHeight).sorted(getDefaultComparator()).collect(Collectors.toList()).subList(0, 1);
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LTE, upperHeight)), 0, 0));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByClauseWithCustomSort() {
        List<T> allExpectedData = getAllLatest();
        List<T> expected = sortByHeightAsc(allExpectedData);
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.LongClause("db_id", DbClause.Op.GT, 0).and(new DbClause.LongClause("db_id", DbClause.Op.LT, Long.MAX_VALUE)), 0, expected.size() - 1, " ORDER BY db_id "));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyOnConnectionWithCache() {
        List<T> allExpectedData = sortByHeightDesc(getAll());
        Map<DbKey, T> keys = new HashMap<>();
        for (int i = 0; i < allExpectedData.size(); i++) {
            T t = allExpectedData.get(i);
            DbKey key = table.getDbKeyFactory().newKey(t);
            if (keys.containsKey(key)) {
                allExpectedData.set(i, keys.get(key));
            } else {
                keys.put(key, t);
            }
        }
        DbUtils.inTransaction(extension, (con) -> {
                    try {
                        PreparedStatement pstm = con.prepareStatement("select * from " + table.getTableName() + " ORDER BY height desc, DB_ID desc ");
                        List<T> all = CollectionUtil.toList(table.getManyBy(con, pstm, true));

                        assertEquals(allExpectedData, all);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }
        );
    }



    @Test
    public void testGetManyOnConnectionWithoutCache() {
        List<T> allExpectedData = sortByHeightDesc(getAll());
        DbUtils.inTransaction(extension, (con) -> {
                    try {
                        PreparedStatement pstm = con.prepareStatement("select * from " + table.getTableName() + " order by height desc, db_id desc");
                        List<T> all = CollectionUtil.toList(table.getManyBy(con, pstm, false));
                        assertEquals(allExpectedData, all);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }
        );
    }

    @Test
    public void testGetAllWithMaxPagination() {
        testGetAllWithPaginationHeightSorted(0, Integer.MAX_VALUE);
        testGetAllWithPaginationCustomSorted(0, Integer.MAX_VALUE);
    }

    @Test
    public void testGetAllWithDataSizePagination() {
        testGetAllWithPaginationHeightSorted(0, getAllLatest().size());
        testGetAllWithPaginationCustomSorted(0, getAllLatest().size());
    }

    @Test
    public void testGetAllWithPaginationExcludingFirst() {
        testGetAllWithPaginationHeightSorted(1, getAllLatest().size());
        testGetAllWithPaginationCustomSorted(1, getAllLatest().size());
    }

    @Test
    public void testGetAllWithPaginationExcludingLast() {
        testGetAllWithPaginationHeightSorted(0, getAllLatest().size() - 1);
        testGetAllWithPaginationCustomSorted(0, getAllLatest().size() - 1);
    }

    @Test
    public void testGetAllWithPaginationExcludingFirstAndLast() {
        testGetAllWithPaginationHeightSorted(1, getAllLatest().size() - 1);
        testGetAllWithPaginationCustomSorted(1, getAllLatest().size() - 1);
    }

    public void testGetAllWithPaginationHeightSorted(int from, int to) {
        testGetAllWithPagination(from, to, DB_ID_HEIGHT_COMPARATOR, DB_ID_HEIGHT_SORT);
    }

    public void testGetAllWithPaginationCustomSorted(int from, int to) {
        testGetAllWithPagination(from, to, getDefaultComparator(), null);
    }

    public void testGetAllWithPagination(int from, int to, Comparator<T> comparator, String sort) {
        List<T> expected = getAllLatest()
                .stream()
                .sorted(comparator)
                .skip(from)
                .limit(to - from)
                .collect(Collectors.toList());

        DbUtils.inTransaction(extension, (con) -> {
            List<T> actual = getAllWithSort(from, to, sort);
            assertEquals(expected, actual);
        });


        List<T> actual = getAllWithSort(from, to, sort);
        assertEquals(expected, actual);

    }

    private List<T> getAllWithSort(int from, int to, String sort) {
        List<T> actual;
        if (StringUtils.isBlank(sort)) {
            actual = CollectionUtil.toList(table.getAll(from, to - 1)); //default sort
        } else {
            actual = CollectionUtil.toList(table.getAll(from, to - 1, sort)); //default
        }
        return actual;
    }

    @Test
    public void testGetAllWithPaginationForMaxHeight() {
        testGetAllWithPaginationForHeight(Integer.MAX_VALUE);
    }

    @Test
    public void testGetAllWithPaginationForLastHeight() {
        Block mock = mock(Block.class);
        doReturn(Integer.MAX_VALUE).when(mock).getHeight();
        getBlockchain().setLastBlock(mock);
        int height = getHeights().get(0);
        testGetAllWithPaginationForHeight(height);
    }

    @Test
    public void testGetAllWithPaginationForMiddleHeight() {
        Block mock = mock(Block.class);
        doReturn(Integer.MAX_VALUE).when(mock).getHeight();
        getBlockchain().setLastBlock(mock);
        List<Integer> heights = getHeights();
        int height = heights.get(heights.size() / 2);
        testGetAllWithPaginationForHeight(height);
    }

    @Test
    public void testGetAllWithPaginationForMinHeight() {
        Block mock = mock(Block.class);
        doReturn(Integer.MAX_VALUE).when(mock).getHeight();
        getBlockchain().setLastBlock(mock);
        List<Integer> heights = getHeights();
        int height = heights.get(heights.size() - 1);
        testGetAllWithPaginationForHeight(height);
    }

    public void testGetAllWithPaginationForHeight(int height) {
        testGetAllWithPaginationForHeight(1, 3, height);
        testGetAllWithPaginationForHeight(0, 2, height);
        testGetAllWithPaginationForHeight(1, 2, height);
        testGetAllWithPaginationForHeight(2, 3, height);
        testGetAllWithPaginationForHeight(0, 1, height);
        testGetAllWithPaginationForHeight(0, Integer.MAX_VALUE, height);
        testGetAllWithPaginationForHeight(1, Integer.MAX_VALUE, height);
    }

    public void testGetAllWithPaginationForHeight(int from, int to, int height) {
        testGetAllWithPaginationForHeightHeightSorted(from, to, height); // check height sort
        testGetAllWithPaginationForHeightCustomSorted(from, to, height); //check default sort
    }

    public void testGetAllWithPaginationForHeightHeightSorted(int from, int to, int height) {
        testGetAllWithPaginationForHeight(from, to, height, DB_ID_HEIGHT_COMPARATOR, DB_ID_HEIGHT_SORT);
    }

    public void testGetAllWithPaginationForHeightCustomSorted(int from, int to, int height) {
        testGetAllWithPaginationForHeight(from, to, height, getDefaultComparator(), null);
    }


    public void testGetAllWithPaginationForHeight(int from, int to, int height, Comparator<T> comp, String sort) {

        List<T> expected = getExpectedAtHeight(from, to, height, comp, (t)->true);

        DbUtils.inTransaction(extension, (con) -> {

            List<T> actual;
            if (StringUtils.isBlank(sort)) {
                actual = CollectionUtil.toList(table.getAll(height, from, to - 1)); // default sort
            } else {
                actual = CollectionUtil.toList(table.getAll(height, from, to - 1, sort)); // custom sort
            }
            //check cache, which should not contain data
            assertEquals(expected, actual);
        });
    }

    protected List<T> getExpectedAtHeight(int from, int to, int height, Comparator<T> comp, Filter<T> filter) {
        List<T> latest = getAllLatest();
        List<T> all = getAll();
        Map<DbKey, List<T>> dbKeyListMap = groupByDbKey(all, table.getDbKeyFactory());
        List<T> expected = all.stream().filter(e -> {
            if (e.getHeight() <= height && filter.test(e)) {
                if (latest.contains(e)) {
                    return true;
                }

                List<T> elements = dbKeyListMap.get(table.getDbKeyFactory().newKey(e));
                boolean notDeleted = elements
                        .stream()
                        .anyMatch(el -> el.getHeight() > height);
                boolean lastAtHeight = elements
                        .stream()
                        .noneMatch(el -> el.getHeight() <= height && el.getHeight() > e.getHeight());
                return notDeleted && lastAtHeight;

            } else {
                return false;
            }
        })
                .sorted(comp)
                .skip(from)
                .limit(to - from)
                .collect(Collectors.toList());
        return expected;
    }


    @Test
    public void testGetCount() {
        int size = getAllLatest().size();
        assertEquals(size, table.getCount());
    }

    @Test
    public void testGetCountByEmptyDbClause() {
        int size = getAllLatest().size();
        assertEquals(size, table.getCount(DbClause.EMPTY_CLAUSE));
    }

    @Test
    public void testGetCountByDbClauseWithSecondElementHeight() {
        testGetCountByDbClause(1);
    }

    @Test
    public void testGetCountByDbClauseWithFirstElementHeight() {
        testGetCountByDbClause(0);
    }

    @Test
    public void testGetCountByDbClauseWithLastElement() {
        List<T> all = sortByHeightDesc(getAllLatest());
        testGetCountByDbClause(all.size() - 1);
    }

    public void testGetCountByDbClause(int index) {
        List<T> all = sortByHeightDesc(getAllLatest());
        for (DbClause.Op op : DbClause.Op.values()) {
//           height
            int height = all.get(index).getHeight();
            int size = (int) all.stream().filter(e -> filterByOperation(op, () -> (long) e.getHeight(), (long) height)).count();
            assertEquals(size, table.getCount(new DbClause.IntClause("height", op, height)));
//            db_id
            long dbId = all.get(index).getDbId();
            size = (int) all.stream().filter(e -> filterByOperation(op, e::getDbId, dbId)).count();
            DbClause.LongClause dbIdClause = new DbClause.LongClause("db_id", op, dbId);
            assertEquals(size, table.getCount(dbIdClause));
//            db_id + height
            for (DbClause.Op op2 : DbClause.Op.values()) {
                size = (int) all.stream().filter(e -> filterByOperation(op, e::getDbId, dbId) && filterByOperation(op2, () -> (long) e.getHeight(), (long) height)).count();
                assertEquals(size, table.getCount(dbIdClause.and(new DbClause.IntClause("height", op2, height))));
            }
        }
    }

    @Test
    public void testGetCountByDbClauseWithLastHeight() {
        Block mock = mock(Block.class);
        doReturn(Integer.MAX_VALUE).when(mock).getHeight();
        getBlockchain().setLastBlock(mock);
        testGetCountByDbClauseWithHeight(0);
    }

    @Test
    public void testGetCountByDbClauseWithNextHeight() {
        Block mock = mock(Block.class);
        doReturn(Integer.MAX_VALUE).when(mock).getHeight();
        getBlockchain().setLastBlock(mock);
        testGetCountByDbClauseWithHeight(1);
    }

    @Test
    public void testGetCountByDbClauseWithMinHeight() {
        Block mock = mock(Block.class);
        doReturn(sortByHeightDesc(getAll()).get(0).getHeight() + 1).when(mock).getHeight();
        getBlockchain().setLastBlock(mock);
        List<T> all = getAllLatest();
        testGetCountByDbClauseWithHeight(all.size() - 1);
    }

    @Test
    public void testGetCountByDbClauseWithMaxHeight() {
        assertEquals(getAllLatest().size(), table.getCount(DbClause.EMPTY_CLAUSE, Integer.MAX_VALUE));
    }

    public void testGetCountByDbClauseWithHeight(int index) {
        List<T> allLatest = getAllLatest();
        for (T el : allLatest) {
            testGetCountByDbClauseWithHeight(index, el.getHeight());
        }
    }


    public void testGetCountByDbClauseWithHeight(int index, int height) {
        List<T> allLatest = getAllLatest();
        List<T> expected = getExpectedAtHeight(0, Integer.MAX_VALUE, height, (v1, v2) -> 0, (v)-> true);
        assertEquals(expected.size(), table.getCount(DbClause.EMPTY_CLAUSE, height));
        for (DbClause.Op op : DbClause.Op.values()) {
            long dbId = allLatest.get(index).getDbId();
            int size = getExpectedAtHeight(0, Integer.MAX_VALUE, height, (v1, v2) -> 0, (v) -> filterByOperation(op, v::getDbId, dbId)).size();
            DbClause.LongClause dbIdClause = new DbClause.LongClause("db_id", op, dbId);
            assertEquals(size, table.getCount(dbIdClause, height));
        }
    }

    public boolean filterByOperation(DbClause.Op op, Supplier<Long> supplier, Long number) {
        switch (op) {
            case EQ:
                return supplier.get().equals(number);
            case GT:
                return supplier.get() > number;
            case LT:
                return supplier.get() < number;
            case NE:
                return !supplier.get().equals(number);
            case GTE:
                return supplier.get() >= number;
            case LTE:
                return supplier.get() <= number;
            default:
                throw new IllegalArgumentException("Db operation is not supported");
        }
    }

    @Test
    public void testGetRowCount() {
        List<T> all = getAll();
        assertEquals(all.size(), table.getRowCount());
    }

    @Test
    public void testInsertOutsideTransaction() {
        Assertions.assertThrows(IllegalStateException.class, () -> table.insert(mock(clazz)));
    }

    @Test
    public void testInsertWhenCachedValueDbKeyEqualsToInsertedButReferencesDiffer() {
        Assertions.assertThrows(IllegalStateException.class, () -> DbUtils.inTransaction(extension, (con) -> {
            T t = getAllLatest().get(0);
            DbKey dbKey = table.getDbKeyFactory().newKey(t);
            table.get(dbKey, true); //add to cache
            T mock = mock(clazz);
            doReturn(dbKey).when(mock).getDbKey();
            table.insert(mock);
        }));
    }

    @Override
    @Test
    public void testInsert() {
        T value = valueToInsert();
        DbUtils.inTransaction(extension, (con) -> {
            table.insert(value);
            assertEquals(value, table.get(table.getDbKeyFactory().newKey(value)));
        });
    }

    @Test
    public void testInsertAlreadyExist() {
        T value = getAllLatest().get(1);
        DbUtils.inTransaction(extension, (con) -> {
            value.setDbId(sortByHeightDesc(getAll()).get(0).getDbId() + 1);
            value.setHeight(value.getHeight() + 1);
            table.insert(value);
            T t = table.get(table.getDbKeyFactory().newKey(value));
            assertEquals(t, value);
        });
        T actual = table.get(table.getDbKeyFactory().newKey(value));
        assertEquals(value, actual);
    }


    public List<T> getDeletedMultiversionRecord() {
        throw new UnsupportedOperationException("deleted multiversion record is not provided");
    }

    Map<DbKey, List<T>> groupByDbKey() {
        return groupByDbKey(table.getDbKeyFactory());
    }

    public List<T> getAllLatest() {
        return sortByHeightDesc(getAll());
    }

    public Comparator<T> getDefaultComparator() {
        return DB_ID_HEIGHT_COMPARATOR;
    }

    public abstract Blockchain getBlockchain();

    public abstract T valueToInsert();
}
