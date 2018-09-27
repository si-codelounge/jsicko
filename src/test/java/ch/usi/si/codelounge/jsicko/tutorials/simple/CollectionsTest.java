package ch.usi.si.codelounge.jsicko.tutorials.simple;

import ch.usi.si.codelounge.jsicko.Contract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CollectionsTest {

    @Test
    public void collectionsSortTest() throws Throwable {
        List<Integer> list = new ArrayList<>(List.of(3,2,1));
        Collections.sort(list);
    }

    @Test
    public void collectionsBadSortTest() throws Throwable {
        List<Integer> list = new ArrayList<>(List.of(3,2,1));
        Executable textFixture = () -> Collections.badSort(list);
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }

    @Test
    public void collectionsBadSort2Test() throws Throwable {
        List<Integer> list = new ArrayList<>(List.of(3,2,1));
        Executable textFixture = () -> Collections.badSort2(list);
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }

}
