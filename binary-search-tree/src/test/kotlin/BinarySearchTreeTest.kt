import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class BinarySearchTreeTest {
    fun <K: Comparable<K>> createTree(vararg keys: K) = BinarySearchTree<K, String>().apply {
        keys.forEach {
            insert(it, it.toString())
        }
    }

    val keyList = arrayOf(5, 3, 4, 15, 7, 6, 8, 20, 1, 2, 17)
    lateinit var tree: BinarySearchTree<Int, String>

    @BeforeEach fun init() {
        tree = createTree(*keyList)

//                                    5
//                    3                                 15
//               1         4                    7               20
//                  2                       6       8       17
    }

    @Test fun height() {
        assertEquals(4, tree.height())
    }

    @Test fun insert() {
        val root = tree.root!!

        assert(root.key == 5)

        assert(root.left?.key == 3)
        root.left!!.let {
            assert(it.left?.key == 1)
            assert(it.left?.right?.key == 2)

            assert(it.right?.key == 4)
        }

        assert(root.right?.key == 15)
        root.right?.let {
            assert(it.left?.key == 7)
            assert(it.left?.left?.key == 6)
            assert(it.left?.right?.key == 8)

            assert(it.right?.key == 20)
            assert(it.right?.left?.key == 17)
        }
    }

    @Test fun removeRoot() {
        with (tree.root!!) {
            assert(key == 5)
            assert(value == "5")
        }

        tree.remove(5)

        with (tree.root!!) {
            assert(key == 6)
            assert(value == "6")
        }

        val tree = createTree(5, 3, 7)

        with (tree.root!!) {
            assert(key == 5)
            assert(value == "5")
        }

        tree.remove(5)

        with (tree.root!!) {
            assert(key == 7)
            assert(value == "7")
        }
    }

    @Test fun removeRootOneChildLeft() {
        val tree = createTree(5, 3)

        with (tree.root!!) {
            assert(key == 5)
            assert(value == "5")
        }

        tree.remove(5)

        with (tree.root!!) {
            assert(key == 3)
            assert(value == "3")
        }
    }

    @Test fun removeRootOneChildRight() {
        val tree = createTree(5, 7)

        with (tree.root!!) {
            assert(key == 5)
            assert(value == "5")
        }

        tree.remove(5)

        with (tree.root!!) {
            assert(key == 7)
            assert(value == "7")
        }
    }

    @Test fun removeRootWithoutChildren() {
        val tree = createTree(5)

        with (tree.root!!) {
            assert(key == 5)
            assert(value == "5")
        }

        tree.remove(5)

        assert(tree.root == null)
    }

    @Test fun removeLeaf() {
        val root = tree.root!!

        assert(root.left!!.left!!.left == null)
        assert(root.left!!.left!!.right != null)

        tree.remove(2)

        assert(root.left!!.left!!.left == null)
        assert(root.left!!.left!!.right == null)
    }

    @Test fun removeNodeWith1Child() {
        val root = tree.root!!

        root.left!!.left!!.let {
            assert(it.key == 1)
            assert(it.value == "1")
        }

        tree.remove(1)

        root.left!!.left!!.let {
            assert(it.key == 2)
            assert(it.value == "2")
        }
    }

    @Test fun removeNodeWith2Children() {
        val root = tree.root!!

        root.right!!.let {
            assert(it.key == 15)
            assert(it.value == "15")

            assert(it.right?.key == 20)
            assert(it.right?.value == "20")

            assert(it.right?.left?.key == 17)
            assert(it.right?.left?.value == "17")
        }

        tree.remove(15)

        root.right!!.let {
            assert(it.key == 17)
            assert(it.value == "17")

            assert(it.right?.key == 20)
            assert(it.right?.value == "20")

            assert(it.right?.left == null)
        }
    }

    @Test fun inOrder() {
        val lst = mutableListOf<Int>()

        tree.inOrder { key, _ ->
            lst.add(key)
        }

        assertArrayEquals(keyList.sortedArray(), lst.toTypedArray())
    }

    @Test fun inOrder2() {
        val lst = mutableListOf<Int>()

        tree.inOrder2 { key, _ ->
            lst.add(key)
        }

        assertArrayEquals(keyList.sortedArray(), lst.toTypedArray())
    }

    @Test fun order() {
        val lst1 = mutableListOf<Int>()
        val lst2 = mutableListOf<Int>()
        val lst3 = mutableListOf<Int>()

        tree.inOrder { key, _ -> lst1.add(key) }
        tree.preOrder { key, _ -> lst2.add(key) }
        tree.postOrder { key, _ -> lst3.add(key) }

        print("IN: "); println(lst1)
        print("PRE: "); println(lst2)
        print("POST: "); println(lst3)
    }

    @Test fun findRoot() {
        val value = tree.find(5)
        assertEquals("5", value)
    }

    @Test fun find14() {
        val value = tree.find(14)
        assertEquals(null, value)
    }

//    @Test fun inOrderBig() {
//        val rand = Random()
//        val keys = mutableListOf<Int>()
//        val tree = BinarySearchTree<Int, String>().apply {
//            for (i in 0..170_000_000) {
//                val r = rand.nextInt()
//                keys.add(r)
//                insert(r, "$r")
//            }
//        }
//
//        val lst = mutableListOf<Int>()
//
//        tree.inOrder2 { key, _ ->
//            lst.add(key)
//            print("$key, ")
//        }
//
//        assertArrayEquals(keys.sorted().toIntArray(), lst.toIntArray())
//    }
}