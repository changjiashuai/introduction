/*
 *   Copyright 2015 Ruben Gees
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.rubengees.introduction;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.rubengees.introduction.adapter.PagerAdapter;
import com.rubengees.introduction.common.DotIndicatorManager;
import com.rubengees.introduction.entity.Option;
import com.rubengees.introduction.entity.Slide;
import com.rubengees.introduction.interfaces.IndicatorManager;
import com.rubengees.introduction.style.Style;
import com.rubengees.introduction.util.ButtonManager;
import com.rubengees.introduction.util.OrientationUtils;

import java.util.ArrayList;

import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_ALLOW_BACK_PRESS;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_ORIENTATION;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_SHOW_INDICATOR;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_SHOW_PREVIOUS_BUTTON;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_SKIP_RESOURCE;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_SKIP_STRING;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_SLIDES;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_STYLE;
import static com.rubengees.introduction.IntroductionBuilder.ORIENTATION_BOTH;
import static com.rubengees.introduction.IntroductionBuilder.ORIENTATION_LANDSCAPE;
import static com.rubengees.introduction.IntroductionBuilder.ORIENTATION_PORTRAIT;

/**
 * The Activity which shows the introduction finally to the user.
 *
 * @author Ruben Gees
 */
public class IntroductionActivity extends AppCompatActivity {

    public static final String OPTION_RESULT = "introduction_option_result";
    public static final String STATE_PREVIOUS_PAGER_POSITION = "previous_pager_position";

    private ArrayList<Slide> slides;
    private Style style;

    private ViewPager pager;
    private ImageButton previous;
    private ImageButton next;
    private FrameLayout indicatorContainer;
    private Button skip;

    private IntroductionConfiguration configuration;

    private ButtonManager buttonManager;
    private IndicatorManager indicatorManager;

    private boolean showPreviousButton;
    private boolean showIndicator;
    private String skipText;
    private boolean allowBackPress;

    private int orientation;

    private int previousPagerPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getFieldsFromBundle();
        applyStyle();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduction_activity);

        configuration = IntroductionConfiguration.getInstance();

        findViews();
        initSlides();
        initManagers();
        initViews();

        if (savedInstanceState == null) {
            select(0);
        } else {
            previousPagerPosition = savedInstanceState.getInt(STATE_PREVIOUS_PAGER_POSITION);

            select(previousPagerPosition);
        }

        //Workaround for fitsSystemWindows in a ViewPager
        ViewCompat.setOnApplyWindowInsetsListener(pager,
                new OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(View v,
                                                                  WindowInsetsCompat insets) {
                        insets = ViewCompat.onApplyWindowInsets(v, insets);

                        if (insets.isConsumed()) {
                            return insets;
                        }

                        boolean consumed = false;

                        if (insets.isConsumed()) {
                            consumed = true;
                        }

                        for (int i = 0, count = pager.getChildCount(); i < count; i++) {
                            ViewCompat.dispatchApplyWindowInsets(pager.getChildAt(i), insets);

                            if (insets.isConsumed()) {
                                consumed = true;
                            }
                        }

                        return consumed ? insets.consumeSystemWindowInsets() : insets;
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_PREVIOUS_PAGER_POSITION, previousPagerPosition);
    }

    @NonNull
    Style getStyle() {
        return style;
    }

    private void getFieldsFromBundle() {
        Bundle bundle = getIntent().getExtras();

        if (bundle == null) {
            bundle = new Bundle();
        }

        slides = bundle.getParcelableArrayList(BUNDLE_SLIDES);
        style = (Style) bundle.getSerializable(BUNDLE_STYLE);
        orientation = bundle.getInt(BUNDLE_ORIENTATION, ORIENTATION_BOTH);
        showPreviousButton = bundle.getBoolean(BUNDLE_SHOW_PREVIOUS_BUTTON, true);
        showIndicator = bundle.getBoolean(BUNDLE_SHOW_INDICATOR, true);
        skipText = bundle.getString(BUNDLE_SKIP_STRING);
        allowBackPress = bundle.getBoolean(BUNDLE_ALLOW_BACK_PRESS, false);

        if (slides == null) {
            slides = new ArrayList<>();
        }

        if (skipText == null) {
            int skipResource = bundle.getInt(BUNDLE_SKIP_RESOURCE, 0);

            if (skipResource != 0) {
                skipText = this.getString(skipResource);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void applyStyle() {
        if (style != null) {
            style.applyStyle(this);

            if (orientation == ORIENTATION_PORTRAIT) {
                OrientationUtils.setOrientationPortrait(this);
            } else if (orientation == ORIENTATION_LANDSCAPE) {
                OrientationUtils.setOrientationLandscape(this);
            }
        }
    }

    private void findViews() {
        ViewGroup root = (ViewGroup) findViewById(R.id.introduction_activity_root);
        pager = (ViewPager) findViewById(R.id.introduction_activity_pager);
        previous = (ImageButton) findViewById(R.id.introduction_activity_button_previous);
        next = (ImageButton) findViewById(R.id.introduction_activity_button_next);
        indicatorContainer = (FrameLayout)
                findViewById(R.id.introduction_activity_container_indicator);
        skip = (Button) findViewById(R.id.introduction_activity_skip);

        if (style != null) {
            style.applyStyleOnActivityView(this, root);
        }
    }

    private void initSlides() {
        for (int i = 0; i < slides.size(); i++) {
            Slide slide = slides.get(i);

            slide.init(this, i);
        }
    }

    private void initManagers() {
        buttonManager = new ButtonManager(previous, next, skip, showPreviousButton,
                skipText != null, slides.size());
        indicatorManager = configuration.getIndicatorManager();

        if (indicatorManager == null) {
            if (showIndicator) {
                indicatorManager = new DotIndicatorManager();
            }
        }

        if (indicatorManager != null) {
            indicatorContainer.addView(indicatorManager.init(LayoutInflater.from(this),
                    indicatorContainer, slides.size()));
        }
    }

    private void initViews() {
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentIndex = pager.getCurrentItem();

                if (currentIndex == slides.size() - 1) {
                    handleFinish();
                } else {
                    pager.setCurrentItem(currentIndex + 1, true);
                }
            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(pager.getCurrentItem() - 1, true);
            }
        });

        pager.setAdapter(new PagerAdapter(getSupportFragmentManager(), slides));
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position != previousPagerPosition) {
                    select(position);

                    IntroductionConfiguration.getInstance().
                            callOnSlideChanged(previousPagerPosition, position);
                    previousPagerPosition = position;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        previous.bringToFront();
        next.bringToFront();

        if (configuration.getPageTransformer() != null) {
            pager.setPageTransformer(true, configuration.getPageTransformer());
        }

        if (skipText != null) {
            skip.setText(skipText);
            skip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleFinish();
                }
            });
        }
    }

    private void select(int position) {
        if (indicatorManager != null) {
            indicatorManager.select(position);
        }

        if (buttonManager != null) {
            buttonManager.select(position);
        }
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() == 0) {
            if (allowBackPress) {
                handleFinishCancelled();
            }
        } else {
            pager.setCurrentItem(pager.getCurrentItem() - 1);
        }
    }

    private void handleFinish() {
        IntroductionConfiguration.destroy();
        ArrayList<Option> options = new ArrayList<>();

        for (Slide slide : slides) {
            if (slide.getOption() != null) {
                options.add(slide.getOption());
            }
        }

        Intent result = new Intent();

        result.putParcelableArrayListExtra(OPTION_RESULT, options);
        setResult(RESULT_OK, result);
        finish();
    }

    private void handleFinishCancelled() {
        IntroductionConfiguration.destroy();
        setResult(RESULT_CANCELED);
        finish();
    }
}
