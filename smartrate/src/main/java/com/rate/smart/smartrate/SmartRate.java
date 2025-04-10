/*
 *  * Copyright (c) 2024 Walid ZOUBIR
 */

package com.rate.smart.smartrate;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.trans.free.translate.smartrate.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SmartRate {



    private String title;
    private String customTitle;

    private Context context;
    private Dialog d;
    private ImageView imgRev1, imgRev2, imgRev3, imgRev4, imgRev5, closeBtn;
    private TextView appTitle, rev1Text, rev2Text, rev3Text, rev4Text, rev5Text;
    private MaterialTextView later_btn;
    private Button ctaBtn;
    private SharedPreferences.Editor editor;

    private String buttonPressedColor;
    private String buttonUnpressedColor;
    private String textPrimaryColor;
    private String textSecondaryColor;
    private String backgroundColor;


    private OnCloseClick onCloseClick;
    private OnFeedbackClick onFeedbackClick;
    private int DAYS_UNTIL_PROMPT = 3; // Default number of days
    private int LAUNCHES_UNTIL_PROMPT = 3; // Default number of launches
    private String EMAIL_ADDRESS, SUBJECT, TEXT_CONTENT;

    private ReviewManager reviewManager;
    private boolean inAppReviewEnabled = false; // Par défaut, c'est désactivé
    private boolean launchReviewDirectly = false; // false signifie que l'on passe par le CTA

    private String feedbackUrl;
    private int launchesUntilPrompt = 2; // Default to 2 launches
    private int showAgainAfterNegative = 5; // Default to 5 launches after negative feedback

    public static SmartRate init(Context context) {
        SmartRate smartRate = new SmartRate();
        smartRate.context = context;
        smartRate.reviewManager = ReviewManagerFactory.create(context);

        // Initialiser les couleurs par défaut depuis les ressources
        smartRate.buttonPressedColor = colorToHex(context.getResources().getColor(R.color.smartrate_button_bg));
        smartRate.buttonUnpressedColor = colorToHex(context.getResources().getColor(R.color.smartrate_gray));
        smartRate.textPrimaryColor = colorToHex(context.getResources().getColor(R.color.smartrate_text_primary));
        smartRate.textSecondaryColor = colorToHex(context.getResources().getColor(R.color.smartrate_text_secondary));
        smartRate.backgroundColor = colorToHex(context.getResources().getColor(R.color.smartrate_white));

        return smartRate;
    }

    // Ajoutez cette méthode d'assistance pour convertir les couleurs int en hex
    private static String colorToHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    public SmartRate setAfterXLaunches(int launches) {
        this.launchesUntilPrompt = launches;
        return this;
    }

    public SmartRate setShowAgainAfterNegative(int launches) {
        this.showAgainAfterNegative = launches;
        return this;
    }

    public void show() {
        d = new Dialog(context, R.style.SmartRateDialog); // Utiliser votre style dédié

        LayoutInflater inflater = LayoutInflater.from(context);
        if (inflater == null)
            return;
        View view = inflater.inflate(R.layout.view_smart_rate, new LinearLayout(context), false);

        applyCustomColors(view);

        bindViews(view);
        initViews();

        d.setContentView(view);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = d.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);
        }
        d.show();
    }


    // Ajoutez aussi cette méthode améliorée pour applyCustomColors
    private void applyCustomColors(View view) {
        // Appliquer les couleurs à différents éléments
        View background = view.findViewById(R.id.main_container);
        if (background != null) {
            background.setBackgroundColor(Color.parseColor(backgroundColor));
        }

        // Pour les textes, appliquer les couleurs
        TextView title = view.findViewById(R.id.love_app);
        if (title != null) {
            title.setTextColor(Color.parseColor(textPrimaryColor));
        }

        // Appliquer les couleurs secondaires aux textes des étoiles
        if (rev1Text != null) rev1Text.setTextColor(Color.parseColor(textSecondaryColor));
        if (rev2Text != null) rev2Text.setTextColor(Color.parseColor(textSecondaryColor));
        if (rev3Text != null) rev3Text.setTextColor(Color.parseColor(textSecondaryColor));
        if (rev4Text != null) rev4Text.setTextColor(Color.parseColor(textSecondaryColor));
        if (rev5Text != null) rev5Text.setTextColor(Color.parseColor(textSecondaryColor));

        // Later button color
        if (later_btn != null) {
            later_btn.setTextColor(Color.parseColor(textSecondaryColor));
        }

        // Pour les boutons CTA
        Button ctaButton = view.findViewById(R.id.cta_btn);
        if (ctaButton != null) {
            if (ctaButton.isEnabled()) {
                // Si le bouton est activé, on utilise la couleur "pressed" (vert)
                GradientDrawable enabledBackground = new GradientDrawable();
                enabledBackground.setColor(Color.parseColor(buttonPressedColor));
                enabledBackground.setCornerRadius(16);
                ctaButton.setBackground(enabledBackground);
            } else {
                // Si le bouton est désactivé, on utilise la couleur "unpressed" (bleu)
                GradientDrawable disabledBackground = new GradientDrawable();
                disabledBackground.setColor(Color.parseColor(buttonUnpressedColor));
                disabledBackground.setCornerRadius(16);
                ctaButton.setBackground(disabledBackground);
            }

            ctaButton.setTextColor(Color.WHITE); // Texte blanc pour contraste
        }

    }
    private void bindViews(View view) {
        imgRev1 = view.findViewById(R.id.rev1_img);
        imgRev2 = view.findViewById(R.id.rev2_img);
        imgRev3 = view.findViewById(R.id.rev3_img);
        imgRev4 = view.findViewById(R.id.rev4_img);
        imgRev5 = view.findViewById(R.id.rev5_img);
        rev1Text = view.findViewById(R.id.rev1_text);
        rev2Text = view.findViewById(R.id.rev2_text);
        rev3Text = view.findViewById(R.id.rev3_text);
        rev4Text = view.findViewById(R.id.rev4_text);
        rev5Text = view.findViewById(R.id.rev5_text);
        ctaBtn = view.findViewById(R.id.cta_btn);
        closeBtn = view.findViewById(R.id.close_btn);
        appTitle = view.findViewById(R.id.love_app);
        later_btn = view.findViewById(R.id.txt_later);
    }

    private static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        String appName;
        try {
            appName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
        } catch (Exception e) {
            appName = context.getString(R.string.smartrate_lib_name);
        }
        // Si l'appName est celui de votre bibliothèque, utilisez la ressource renommée
        if (appName.equals(context.getString(R.string.smartrate_lib_name))) {
            try {
                // Tenter d'obtenir le vrai nom de l'application hôte
                PackageManager packageManager = context.getPackageManager();
                String hostAppName = packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(context.getPackageName(), 0)).toString();
                return hostAppName;
            } catch (Exception e) {
                return appName;
            }
        }

        return appName;
    }

    private void sendEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        String uriText = "mailto:" + EMAIL_ADDRESS + "?subject=" + Uri.encode(SUBJECT) + "&body=" + Uri.encode(TEXT_CONTENT);
        emailIntent.setData(Uri.parse(uriText));
        if (emailIntent.resolveActivity(context.getPackageManager()) != null) {
            try {
                context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(context, context.getResources().getString(R.string.no_email_client), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initViews() {
        String titleText;
        if (customTitle != null && !customTitle.isEmpty()) {
            titleText = customTitle;
        } else {
            titleText = context.getString(R.string.love_this_app);
        }
        appTitle.setText(titleText);
        setGrayStars(); // Initialement, toutes les étoiles sont grises

        // Désactiver explicitement le bouton au démarrage
        ctaBtn.setEnabled(false);

        // Créer une copie teintée du drawable round_corners_fill avec votre couleur bleue
        GradientDrawable disabledBackground = (GradientDrawable) context.getResources()
                .getDrawable(R.drawable.round_corners_fill).mutate();
        disabledBackground.setColor(Color.parseColor(buttonUnpressedColor)); // Couleur bleue
        ctaBtn.setBackground(disabledBackground);

        imgRev1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStarColors(1);
                updateCtaButton(1);
                // Notifier via le listener d'évaluation (même pour 1 étoile)
                if (onRateClick != null) {
                    onRateClick.onRateClickListener();
                }
            }
        });

        imgRev2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStarColors(2);
                updateCtaButton(2);
                // Notifier via le listener d'évaluation (même pour 2 étoiles)
                if (onRateClick != null) {
                    onRateClick.onRateClickListener();
                }
            }
        });

        imgRev3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStarColors(3);
                updateCtaButton(3);
                // Notifier via le listener d'évaluation (même pour 3 étoiles)
                if (onRateClick != null) {
                    onRateClick.onRateClickListener();
                }
            }
        });

        imgRev4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStarColors(4);
                // Notifier via le listener d'évaluation
                if (onRateClick != null) {
                    onRateClick.onRateClickListener();
                }
                handleReviewOrFeedback();
                recordPositiveReview(context);
                d.dismiss();
            }
        });

        imgRev5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStarColors(5);
                // Notifier via le listener d'évaluation
                if (onRateClick != null) {
                    onRateClick.onRateClickListener();
                }
                handleReviewOrFeedback();
                recordPositiveReview(context);
                d.dismiss();
            }
        });

        later_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("RemindMeLater", true);
                editor.apply();
                d.dismiss();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onCloseClick != null)
                    onCloseClick.onCloseClickListener();
                else
                    recordCloseWithoutAction(context);

                d.dismiss();
            }
        });
    }

    private void setGrayStars() {
        imgRev1.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.layer_star, null));
        imgRev2.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.layer_star_2, null));
        imgRev3.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.layer_star_3, null));
        imgRev4.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.layer_star_4, null));
        imgRev5.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.layer_star_5, null));

        resetTextStyleAndColor(rev1Text);
        resetTextStyleAndColor(rev2Text);
        resetTextStyleAndColor(rev3Text);
        resetTextStyleAndColor(rev4Text);
        resetTextStyleAndColor(rev5Text);

        // Désactiver le bouton et appliquer la couleur par défaut
        ctaBtn.setEnabled(false);

        // Utiliser le drawable défini dans le XML comme base et le teinter avec la couleur désactivée
        GradientDrawable disabledBackground = (GradientDrawable) context.getResources()
                .getDrawable(R.drawable.round_corners_fill_gray).mutate();
        ctaBtn.setBackground(disabledBackground);
        ctaBtn.setTextColor(context.getResources().getColor(R.color.smartrate_dim));
    }

    private void resetTextStyleAndColor(TextView textView) {
        textView.setTextColor(Color.BLACK);
        textView.setTypeface(null, Typeface.NORMAL);
    }

    private void setStarColors(int starCount) {
        setGrayStars(); // Réinitialise toutes les étoiles en gris

        for (int i = 1; i <= starCount; i++) {
            int color = getColorForStar(starCount);
            setStarToColor(getImageViewForStar(i), color);
        }

        highlightSelectedText(starCount);
    }

    private int getColorForStar(int starCount) {
        switch (starCount) {
            case 1:
                return context.getResources().getColor(R.color.smartrate_star1_color);
            case 2:
                return context.getResources().getColor(R.color.smartrate_star2_color);
            case 3:
                return context.getResources().getColor(R.color.smartrate_star3_color);
            case 4:
                return context.getResources().getColor(R.color.smartrate_star4_color);
            case 5:
                return context.getResources().getColor(R.color.smartrate_star5_color);
            default:
                return context.getResources().getColor(R.color.smartrate_grey_star);
        }
    }



    private ImageView getImageViewForStar(int starIndex) {
        switch (starIndex) {
            case 1:
                return imgRev1;
            case 2:
                return imgRev2;
            case 3:
                return imgRev3;
            case 4:
                return imgRev4;
            case 5:
                return imgRev5;
            default:
                return null;
        }
    }

    private void setStarToColor(ImageView imageView, int color) {
        LayerDrawable layerDrawable = (LayerDrawable) imageView.getDrawable();
        if (layerDrawable != null) {
            layerDrawable.findDrawableByLayerId(R.id.star_background).setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                    0, 0, 0, 0, Color.red(color),
                    0, 0, 0, 0, Color.green(color),
                    0, 0, 0, 0, Color.blue(color),
                    0, 0, 0, 1, 0
            })));
            imageView.setImageDrawable(layerDrawable);
        }
    }

    private void highlightSelectedText(int starCount) {
        if (starCount == 1) {
            setTextStyleAndColor(rev1Text, Color.parseColor("#ED3450"));
        } else if (starCount == 2) {
            setTextStyleAndColor(rev2Text, Color.parseColor("#FF801E"));
        } else if (starCount == 3) {
            setTextStyleAndColor(rev3Text, Color.parseColor("#FFD12C"));
        } else if (starCount == 4) {
            setTextStyleAndColor(rev4Text, Color.parseColor("#ADDB17"));
        } else if (starCount == 5) {
            setTextStyleAndColor(rev5Text, Color.parseColor("#4ECF58"));
        }
    }

    private void setTextStyleAndColor(TextView textView, int color) {
        textView.setTextColor(color);
        textView.setTypeface(null, Typeface.BOLD);
    }

    // Modifiez la méthode updateCtaButton pour utiliser les ressources directement
    private void updateCtaButton(int starCount) {
        if (starCount <= 3) {
            ctaBtn.setText(context.getResources().getText(R.string.write_feedback));
            ctaBtn.setEnabled(true);

            // Créer un drawable explicite au lieu d'utiliser celui du XML
            GradientDrawable enabledBackground = new GradientDrawable();
            enabledBackground.setColor(Color.parseColor(buttonPressedColor));
            enabledBackground.setCornerRadius(16);

            // Forcer l'application du drawable comme background
            ctaBtn.setBackground(enabledBackground);

            // Forcer la couleur du texte
            ctaBtn.setTextColor(Color.WHITE);

            ctaBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFeedbackDialog();
                    d.dismiss();
                    recordNegativeFeedback(context);
                }
            });
        } else {
            if (onRateClick != null) {
                onRateClick.onRateClickListener();
            }
            handleReviewOrFeedback();
            recordPositiveReview(context);
            dontShowAgain(true, context);
            d.dismiss();
        }
    }

    private void handleReviewOrFeedback() {
        if (inAppReviewEnabled) {
            if (launchReviewDirectly) {
                launchInAppReview();
            } else {
                prepareForReviewViaCTA();
            }
        } else {
            redirectToPlayStore();
        }
    }

    private void prepareForReviewViaCTA() {
        ctaBtn.setText(context.getResources().getString(R.string.rate));
        ctaBtn.setEnabled(true);
        ctaBtn.setVisibility(View.VISIBLE); // Assurez-vous que le bouton est visible
        ctaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchInAppReview();
            }
        });
    }

    private void launchInAppReview() {
        d.dismiss();
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = reviewManager.launchReviewFlow((Activity) context, reviewInfo);
                flow.addOnCompleteListener(task2 -> {
                    SmartRate.dontShowAgain(true, context);
                    SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("InAppReviewShown", true);
                    editor.apply();
                });
            }
        });
    }

    private void showSuccesDialog() {
        final Dialog succesDialog = new Dialog(context);
        succesDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater inflater = LayoutInflater.from(context);
        if (inflater == null) return;
        View succesView = inflater.inflate(R.layout.view_succes, null);
        applyCustomColors(succesView);

        succesDialog.setContentView(succesView);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        Window window = succesDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Fond transparent
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.BOTTOM;
            window.setAttributes(layoutParams);
        }

        ImageView close_btn = succesView.findViewById(R.id.close_btn);

        close_btn.setOnClickListener(v -> succesDialog.dismiss());

        succesDialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(succesDialog::dismiss, 3000);
    }

    private void showFeedbackDialog() {
        final Dialog feedbackDialog = new Dialog(context);
        feedbackDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater inflater = LayoutInflater.from(context);
        if (inflater == null) return;
        View feedbackView = inflater.inflate(R.layout.view_feedback, null);

        // Appliquer les couleurs personnalisées
        applyCustomColors(feedbackView);

        feedbackDialog.setContentView(feedbackView);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        Window window = feedbackDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.BOTTOM;
            window.setAttributes(layoutParams);
        }

        final EditText feedbackMessage = feedbackView.findViewById(R.id.feedback_message);
        Button submitButton = feedbackView.findViewById(R.id.feedback_submit);
        ImageView cancelButton = feedbackView.findViewById(R.id.feedback_cancel);

        // Initialiser le bouton submit en état désactivé avec la couleur bleue
        submitButton.setEnabled(false);


        GradientDrawable disabledBackground = new GradientDrawable();
        disabledBackground.setColor(Color.parseColor(buttonUnpressedColor));
        disabledBackground.setCornerRadius(16);

        submitButton.setBackground(disabledBackground);
        submitButton.setTextColor(Color.WHITE);

        feedbackMessage.setHint(R.string.write_tour_feedback_here);

        feedbackMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    feedbackMessage.setHint("");
                } else {
                    if (feedbackMessage.getText().toString().isEmpty()) {
                        feedbackMessage.setHint(R.string.write_tour_feedback_here);
                    }
                }
            }
        });

        submitButton.setOnClickListener(v -> {
            String message = feedbackMessage.getText().toString();
            if (!message.isEmpty()) {
                sendFeedback(message);
                recordNegativeFeedback(context);

                SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("dontshowagain", true);
                editor.apply();

                if (onFeedbackClick != null) {
                    onFeedbackClick.onFeedBackClickListener();
                }
            }
            feedbackDialog.dismiss();
            showSuccesDialog();
        });

        feedbackMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }


            @Override
            public void afterTextChanged(Editable s) {
                boolean isEnabled = s.length() > 0;
                submitButton.setEnabled(isEnabled);

                if (isEnabled) {
                    // Bouton activé - créer un nouveau drawable avec la couleur explicite
                    GradientDrawable enabledBackground = new GradientDrawable();
                    enabledBackground.setColor(Color.parseColor(buttonPressedColor));
                    enabledBackground.setCornerRadius(16);
                    submitButton.setBackground(enabledBackground);
                } else {
                    // Bouton désactivé - créer un nouveau drawable avec la couleur explicite
                    GradientDrawable disabledBackground = new GradientDrawable();
                    disabledBackground.setColor(Color.parseColor(buttonUnpressedColor));
                    disabledBackground.setCornerRadius(16);
                    submitButton.setBackground(disabledBackground);
                }
            }
        });

        cancelButton.setOnClickListener(v -> feedbackDialog.dismiss());

        feedbackDialog.show();
    }

    private void sendFeedback(final String feedback) {
        if (feedbackUrl == null || feedbackUrl.isEmpty()) {
            throw new IllegalStateException("Feedback URL is not set. Use setUrlFeedback() to set it.");
        }

        StringRequest postRequest = new StringRequest(Request.Method.POST, feedbackUrl,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            //   Toast.makeText(context, "Feedback envoyé avec succès", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Erreur d'envoi", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Erreur lors du traitement de la réponse", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(context, "Erreur réseau: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("feedback", feedback);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };

        Volley.newRequestQueue(context).add(postRequest);
    }

    public SmartRate setUrlFeedback(String url) {
        this.feedbackUrl = url;
        return this;
    }

    private void redirectToPlayStore() {
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName())));
        if (editor != null) {
            editor.putBoolean("dontshowagain", true);
            editor.apply();
        }
        d.dismiss();
    }

    public SmartRate setInAppReviewEnabled(boolean enabled) {
        this.inAppReviewEnabled = enabled;
        return this;
    }

    public void build() {
        SharedPreferences prefs = context.getSharedPreferences("apprater", 0);
        if (prefs.getBoolean("dontshowagain", false)) {
            return;
        }

        editor = prefs.edit();

        boolean remindMeLater = prefs.getBoolean("RemindMeLater", false);

        // Si l'utilisateur a cliqué sur "Remind Me Later", nous affichons le dialogue la prochaine fois
        if (remindMeLater) {
            editor.putBoolean("RemindMeLater", false); // Réinitialiser pour qu'il ne soit pas bloqué
            editor.apply();
            show();
            return;
        }

        // Increment launch counter
        long launch_count = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launch_count);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }

        // Check if the user has previously given a rating of 4 or 5 stars
        boolean positiveReviewGiven = prefs.getBoolean("positiveReviewGiven", false);
        if (positiveReviewGiven) {
            return; // Don't show the dialog if a positive review was already given
        }

        // Check if the user has given negative feedback recently
        long lastNegativeFeedbackTime = prefs.getLong("lastNegativeFeedbackTime", 0);
        if (System.currentTimeMillis() < lastNegativeFeedbackTime + (showAgainAfterNegative * 24 * 60 * 60 * 1000L)) {
            return; // Don't show the dialog if negative feedback was given recently
        }

        // Wait until the specified number of launches before opening
        if (launch_count >= launchesUntilPrompt) {
            show();
        }
        editor.apply();
    }

    public SmartRate setDaysDelay(int numberOfDays) {
        DAYS_UNTIL_PROMPT = numberOfDays;
        return this;
    }

    public SmartRate setLaunchesDelay(int numberOfLaunches) {
        LAUNCHES_UNTIL_PROMPT = numberOfLaunches;
        return this;
    }

    public SmartRate setMailingContact(String emailAddress, String subject, String textContent) {
        EMAIL_ADDRESS = emailAddress;
        SUBJECT = subject;
        TEXT_CONTENT = textContent;
        return this;
    }

    public SmartRate setLaunchReviewDirectly(boolean launchDirectly) {
        this.launchReviewDirectly = launchDirectly;
        return this;
    }

    public static void resetDelay(Context context) {
        SharedPreferences.Editor editor;
        SharedPreferences prefs = context.getSharedPreferences("apprater", 0);
        editor = prefs.edit();
        editor.putBoolean("dontshowagain", false);
        editor.putLong("date_firstlaunch", 0);
        editor.putLong("launch_count", 0);
        editor.apply();
    }

    public static void dontShowAgain(Boolean dontShowAgain, Context context) {
        SharedPreferences.Editor editor;
        SharedPreferences prefs = context.getSharedPreferences("apprater", 0);
        editor = prefs.edit();
        editor.putBoolean("dontshowagain", dontShowAgain);
        editor.apply();
    }

    private void recordNegativeFeedback(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("apprater", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("lastNegativeFeedbackTime", System.currentTimeMillis());
        editor.putBoolean("DialogClosedWithoutAction", false);
        editor.apply();
    }

    private void recordPositiveReview(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("apprater", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("positiveReviewGiven", true);
        editor.putBoolean("DialogClosedWithoutAction", false);
        editor.apply();
    }

    private void recordCloseWithoutAction(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("apprater", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("DialogClosedWithoutAction", true);
        editor.putLong("lastCloseWithoutActionTime", System.currentTimeMillis());
        editor.apply();
    }

    public SmartRate setColorButtonPressed(String hexColor) {
        this.buttonPressedColor = hexColor;
        return this;
    }

    public SmartRate setColorButtonUnpressed(String hexColor) {
        this.buttonUnpressedColor = hexColor;
        return this;
    }

    public SmartRate setColorTextPrimary(String hexColor) {
        this.textPrimaryColor = hexColor;
        return this;
    }

    public SmartRate setColorTextSecondary(String hexColor) {
        this.textSecondaryColor = hexColor;
        return this;
    }

    public SmartRate setColorBackground(String hexColor) {
        this.backgroundColor = hexColor;
        return this;
    }

    public interface OnCloseClick {
        void onCloseClickListener();
    }

    public SmartRate setOnCloseClickListener(OnCloseClick onCloseClick) {
        this.onCloseClick = onCloseClick;
        return this;
    }

    public SmartRate setOnFeedbackClickListener(OnFeedbackClick onFeedbackClick) {
        this.onFeedbackClick = onFeedbackClick;
        return this;
    }
    public SmartRate setTitle(String title) {
        this.customTitle = title;
        return this;
    }
    // 1. Ajouter les deux interfaces pour gérer les deux types d'évaluations
    public interface OnRateClick {
        void onRateClickListener();
    }

    public interface OnFeedbackClick {
        void onFeedBackClickListener();
    }

    // 2. Ajouter un champ pour stocker le listener d'évaluation positive
    private OnRateClick onRateClick;

    // 3. Ajouter la méthode pour définir le listener d'évaluation positive
    public SmartRate setOnRateClickListener(OnRateClick onRateClick) {
        this.onRateClick = onRateClick;
        return this;
    }
}
