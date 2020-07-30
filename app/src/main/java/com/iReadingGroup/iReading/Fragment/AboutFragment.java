package com.iReadingGroup.iReading.Fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.danielstone.materialaboutlibrary.MaterialAboutFragment;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.iReadingGroup.iReading.Activity.SettingsActivity;
import com.iReadingGroup.iReading.R;

import java.net.URLEncoder;

/**
 * The type About fragment.
 */
public class AboutFragment extends MaterialAboutFragment {
    /**
     * The constant ALIPAY_PERSON.
     */
    public static final String ALIPAY_PERSON = "HTTPS://QR.ALIPAY.COM/FKX06332TUSRAIPEXK1818";
    private MaterialAboutList mal = new MaterialAboutList();

    /**
     * Open alipay pay page boolean.
     *
     * @param context the context
     * @param qrcode  the qrcode
     * @return the boolean
     */
    public static boolean openAlipayPayPage(Context context, String qrcode) {
        try {
            qrcode = URLEncoder.encode(qrcode, "utf-8");
        } catch (Exception e) {
        }
        try {
            final String alipayqr = "alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=" + qrcode;
            openUri(context, alipayqr + "%3F_s%3Dweb-other&_t=" + System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 发送一个intent
     *
     * @param context
     * @param s
     */
    private static void openUri(Context context, String s) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
        context.startActivity(intent);
    }

    @Override
    protected MaterialAboutList getMaterialAboutList(final Context activityContext) {
        if (mal.getCards().size() == 0) {
            MaterialAboutCard.Builder appCardBuilder = new MaterialAboutCard.Builder();


            appCardBuilder.addItem(new MaterialAboutTitleItem.Builder()
                    .text("iReading")
                    .desc("I Was Enchanted To Meet You! ")
                    .icon(R.mipmap.icon)
                    .build());
            appCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                    .text("Version")
                    .subText("1.0.0")
                    .build());
            MaterialAboutCard.Builder authorCardBuilder = new MaterialAboutCard.Builder();
            authorCardBuilder.title("Author");

            authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                    .text("Kristy")
                    .subText("Please keep looking and don't settle")
                    .icon(R.mipmap.myface)
                    .build());
            authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                    .text("However bad life may seem,there is always something you can do,and succeed at")
                    .subText("作者寄语")
                    .icon(R.mipmap.pay)
                    .setOnClickAction(new MaterialAboutItemOnClickAction() {
                        @Override
                        public void onClick() {
                            openAliPay2Pay(ALIPAY_PERSON);
                        }
                    })
                    .build());

            MaterialAboutCard.Builder settingbuilder = new MaterialAboutCard.Builder();
            settingbuilder.addItem(new MaterialAboutActionItem.Builder()
                    .text("设置")
                    .icon(R.mipmap.clearcache)
                    .setOnClickAction(new MaterialAboutItemOnClickAction() {
                        @Override
                        public void onClick() {
                            Intent intent = new Intent(getActivity(), SettingsActivity.class);

                            startActivity(intent);

                        }
                    })
                    .build());
            mal.addCard(appCardBuilder.build());
            mal.addCard(authorCardBuilder.build());
            mal.addCard(settingbuilder.build());
        }
        return mal; // This creates an empty screen, add cards with .addCard()
    }

    @Override
    protected int getTheme() {
        return R.style.AppTheme_MaterialAboutActivity_Fragment;
    }

    /**
     * 支付
     *
     * @param qrCode
     */
    private void openAliPay2Pay(String qrCode) {
        if (openAlipayPayPage(getContext(), qrCode)) {
            Toast.makeText(getContext(), "跳转成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "跳转失败", Toast.LENGTH_SHORT).show();
        }
    }

}