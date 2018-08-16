class Node<K: Comparable<K>, V>(
        var key: K,
        var value: V,
        var left: Node<K, V>? = null,
        var right: Node<K, V>? = null


) {
    override fun toString(): String {
        return "Node(key=$key, left=${left?.key}, right=${right?.key})"
    }
}

typealias NodePair<K, V> = Pair<Node<K, V>?, Node<K, V>?>

class BinarySearchTree<K: Comparable<K>, V> {
    var root: Node<K, V>? = null

    private fun find(root: Node<K, V>?, key: K): NodePair<K, V> {
        var node: Node<K, V>? = root
        var parent: Node<K, V>? = null

        while (node != null) {
            val compare = key.compareTo(node.key)

            if (compare == 0)
                break

            parent = node
            node = if (compare > 0) node.right else node.left
        }

        return Pair(node, parent)
    }

    fun insert(key: K, value: V) {
        val (node, parent) = find(root, key)

        when {
            node != null -> node.value = value
            parent == null -> root = Node(key, value)
            else -> {
                val compare = key.compareTo(parent.key)

                if (compare > 0) {
                    parent.right = Node(key, value)
                } else {
                    parent.left = Node(key, value)
                }
            }
        }
    }

    fun remove(key: K) {
        val (n, p) = find(root, key)

        if (n != null) {
            // both children
            if (n.left != null && n.right != null) {
                var leftest: Node<K, V> = n.right!!
                var leftestParent: Node<K, V>? = null

                while (leftest.left != null) {
                    leftestParent = leftest
                    leftest = leftest.left!!
                }

                leftestParent?.left = null

                n.key = leftest.key
                n.value = leftest.value
            } else {
                when {
                    // only left child
                    n.left != null -> {
                        n.key = n.left!!.key
                        n.value = n.left!!.value
                        n.left = n.left!!.left
                    }
                    // only right child
                    n.right != null -> {
                        n.key = n.right!!.key
                        n.value = n.right!!.value
                        n.right = n.right!!.left
                    }
                    // no children
                    else -> when {
                        p == null -> this.root = null // remove root
                        p.left == n -> p.left = null // remove left child
                        else -> p.right = null // remove right child
                    }
                }
            }
        }
    }

    fun find(key: K): V? = find(root, key).first?.value

    fun inOrder(f: (K, V) -> Unit) = inOrder(root, f)
    fun inOrder2(f: (K, V) -> Unit) = inOrder2(root, f)
    fun preOrder(f: (K, V) -> Unit) = preOrder(root, f)
    fun postOrder(f: (K, V) -> Unit) = postOrder(root, f)

    private fun inOrder(root: Node<K, V>?, f: (K, V) -> Unit) {
        if (root != null) {
            inOrder(root.left, f)
            f.invoke(root.key, root.value)
            inOrder(root.right, f)
        }
    }

    private fun inOrder2(root: Node<K, V>?, f: (K, V) -> Unit) {
        if (root == null)
            return

        val nodes = mutableListOf(root)

        while (nodes.isNotEmpty()) {
            val node = nodes.last()
            val left = node.left
            val right = node.right

            if (left != null) {
                nodes.add(left)
                continue
            }

            f.invoke(node.key, node.value)
            nodes.removeAt(nodes.lastIndex)

            if (right != null) {
                nodes.add(right)
                continue
            }
        }
    }

    private fun preOrder(root: Node<K, V>?, f: (K, V) -> Unit) {
        if (root != null) {
            f.invoke(root.key, root.value)
            preOrder(root.left, f)
            preOrder(root.right, f)
        }
    }

    private fun postOrder(root: Node<K, V>?, f: (K, V) -> Unit) {
        if (root != null) {
            preOrder(root.left, f)
            preOrder(root.right, f)
            f.invoke(root.key, root.value)
        }
    }

    fun height(): Int = height(root)

    private fun height(node: Node<K, V>?): Int {
        return if (node == null)
            0
        else
            1 + Math.max(height(node.left), height(node.right))
    }
}