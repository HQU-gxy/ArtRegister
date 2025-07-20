package redstone.artregister

class PieceInfo(
    val pieceName: String, val creatorName: String, val ownerName: String
) {
    override fun toString(): String {
        return "Piece Name: $pieceName, Creator: $creatorName, Owner: $ownerName"
    }
}