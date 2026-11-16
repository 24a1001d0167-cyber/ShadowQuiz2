package com.example.shadowquiz;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing one animal puzzle with habitat category, identification hint, and fun biological fact.
 */
public class AnimalPuzzle {
    private final String animalName;
    private final int imageResId;
    private final String category;
    private final String hint;
    private final String funFact;

    public AnimalPuzzle(String animalName, int imageResId, String category, String hint, String funFact) {
        this.animalName = animalName;
        this.imageResId = imageResId;
        this.category = category;
        this.hint = hint;
        this.funFact = funFact;
    }

    public String getAnimalName() {
        return animalName;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getCategory() {
        return category;
    }

    public String getHint() {
        return hint;
    }

    public String getFunFact() {
        return funFact;
    }

    /**
     * Returns all 16 animals (5 original PNGs + 11 high-contrast vector silhouettes).
     */
    public static List<AnimalPuzzle> getAllAnimals() {
        List<AnimalPuzzle> list = new ArrayList<>();
        // 5 original PNGs
        list.add(new AnimalPuzzle("Con Chó", R.drawable.con_cho_removebg_preview,
                "Thú cưng",
                "Người bạn trung thành hay vẫy đuôi chào chủ",
                "Mũi chó sở hữu tới 300 triệu tế bào khứu giác, nhạy gấp 40 lần con người và có dấu mũi độc nhất như vân tay."));
        list.add(new AnimalPuzzle("Con Dê", R.drawable.con_de_removebg_preview,
                "Nông trại",
                "Có cặp sừng cong, chòm râu cằm và kêu be be",
                "Đồng tử mắt dê có hình chữ nhật nằm ngang giúp chúng có tầm nhìn rộng tới 320 độ để phát hiện kẻ săn mồi."));
        list.add(new AnimalPuzzle("Con Khỉ", R.drawable.con_khi_removebg_preview,
                "Hoang dã",
                "Rất tinh nghịch, thích leo trèo và ăn chuối",
                "Nhiều loài khỉ biết dùng đá đập vỏ hạt cứng và tự chế tạo công cụ bắt côn trùng bằng cành cây."));
        list.add(new AnimalPuzzle("Con Kiến", R.drawable.con_kien_removebg_preview,
                "Côn trùng",
                "Nhỏ bé nhưng chăm chỉ, sống thành bầy đàn và có râu dài",
                "Kiến có thể nâng vật nặng gấp 20 đến 50 lần trọng lượng cơ thể và giao tiếp bằng tín hiệu hóa học pheromone."));
        list.add(new AnimalPuzzle("Con Sóc", R.drawable.con_soc_removebg_preview,
                "Hoang dã",
                "Chiếc đuôi xù cực lớn và mê mẩn hạt dẻ",
                "Sóc trồng hàng nghìn cây xanh mỗi năm một cách tình cờ vì chúng chôn hạt dẻ rồi quên mất một phần vị trí!"));

        // 11 vector animals
        list.add(new AnimalPuzzle("Con Mèo", R.drawable.animal_cat,
                "Thú cưng",
                "Đôi tai nhọn, bộ ria mép và thích bắt chuột",
                "Mèo dành khoảng 70% cuộc đời để ngủ và có xương đòn linh hoạt giúp chui lọt bất kỳ khe hở nào vừa đầu chúng."));
        list.add(new AnimalPuzzle("Con Thỏ", R.drawable.animal_rabbit,
                "Thú cưng",
                "Đôi tai dài vểnh lên, đuôi tròn nhỏ và thích cà rốt",
                "Đôi tai dài của thỏ vừa giúp nghe âm thanh từ xa vừa hoạt động như bộ tản nhiệt làm mát cơ thể."));
        list.add(new AnimalPuzzle("Con Voi", R.drawable.animal_elephant,
                "Hoang dã",
                "Động vật trên cạn to lớn với chiếc vòi dài và ngà trắng",
                "Chiếc vòi voi chứa hơn 40.000 cơ bắp nhỏ, vừa nhấc được thân cây nặng vừa nhặt được hạt lạc nhỏ xíu."));
        list.add(new AnimalPuzzle("Con Rùa", R.drawable.animal_turtle,
                "Bò sát",
                "Mang chiếc mai cứng trên lưng và bước đi chậm rãi",
                "Mai rùa thực chất hợp nhất từ xương sườn và cột sống của chúng, nên rùa không bao giờ có thể bò ra khỏi mai."));
        list.add(new AnimalPuzzle("Con Cú Mèo", R.drawable.animal_owl,
                "Chim muông",
                "Đôi mắt tròn xoe cực thính vào ban đêm",
                "Cú mèo có thể xoay đầu tới 270 độ và đôi cánh có răng cưa mềm giúp bay săn mồi hoàn toàn không phát tiếng động."));
        list.add(new AnimalPuzzle("Con Cua", R.drawable.animal_crab,
                "Đại dương",
                "Có 8 chân bò ngang và 2 chiếc càng kẹp cực khỏe",
                "Răng của một số loài cua nằm ngay bên trong dạ dày giúp nghiền thức ăn sau khi nuốt!"));
        list.add(new AnimalPuzzle("Con Cá Heo", R.drawable.animal_dolphin,
                "Đại dương",
                "Thông minh, thân thiện và hay nhảy vọt lên mặt sóng",
                "Cá heo ngủ chỉ với một nửa bộ não tại một thời điểm để nửa còn lại canh chừng và điều khiển việc ngoi lên thở."));
        list.add(new AnimalPuzzle("Chim Cánh Cụt", R.drawable.animal_penguin,
                "Chim muông",
                "Sống ở vùng băng giá, dáng đi lạch bạch cực đáng yêu",
                "Tuy không bay trên trời, chim cánh cụt có thể bơi dưới nước với vận tốc lên tới 35 km/h như đang bay dưới biển."));
        list.add(new AnimalPuzzle("Con Bướm", R.drawable.animal_butterfly,
                "Côn trùng",
                "Đôi cánh rực rỡ đối xứng, thường bay lượn bên hoa",
                "Bướm nếm vị thức ăn bằng các thụ thể cảm giác ở đầu bàn chân khi đậu xuống bông hoa!"));
        list.add(new AnimalPuzzle("Con Ếch", R.drawable.animal_frog,
                "Lưỡng cư",
                "Hai mắt lồi trên đỉnh đầu, bật nhảy xa và kêu ộp ộp",
                "Ếch không uống nước bằng miệng mà hấp thụ toàn bộ lượng nước cần thiết qua làn da ẩm mượt của mình."));
        list.add(new AnimalPuzzle("Hươu Cao Cổ", R.drawable.animal_giraffe,
                "Hoang dã",
                "Sở hữu chiếc cổ dài nhất thế giới để ăn lá trên cao",
                "Dù cổ dài tới hơn 2 mét, hươu cao cổ chỉ có đúng 7 đốt sống cổ giống hệt con người — nhưng mỗi đốt dài gần 25 cm."));
        return list;
    }

    /**
     * Returns 8 drawable IDs usable as user profile avatars.
     */
    public static int[] getAvatarResIds() {
        return new int[]{
                R.drawable.animal_cat,
                R.drawable.animal_rabbit,
                R.drawable.animal_owl,
                R.drawable.animal_penguin,
                R.drawable.animal_dolphin,
                R.drawable.animal_frog,
                R.drawable.con_cho_removebg_preview,
                R.drawable.con_soc_removebg_preview
        };
    }
}
