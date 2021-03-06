package com.hyuj.feelendar.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hyuj.feelendar.R;
import com.hyuj.feelendar.calendar.decorator.FeelDayDecorator;
import com.hyuj.feelendar.calendar.decorator.SaturdayDecorator;
import com.hyuj.feelendar.calendar.decorator.SundayDecorator;
import com.hyuj.feelendar.component.FeelMappingComponent;
import com.hyuj.feelendar.domain.Diary;
import com.hyuj.feelendar.domain.Feel;
import com.hyuj.feelendar.helper.DatabaseAccessHelper;
import com.hyuj.feelendar.helper.SQLiteAccessHelper;
import com.hyuj.feelendar.util.DateConvertUtil;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    DatabaseAccessHelper db;
    FeelMappingComponent feelMappingComponent = FeelMappingComponent.getInstance();
    List<FeelDayDecorator> feelDayDecoratorList = new ArrayList<>();
    List<Diary> currentYearDiaryList;
    List<Feel> feelList;

    /**
     * create 시 필요한 데이터를 로드하고, 초기화합니다.
     * */
    private void init(Context context){
        // DB Helper 초기화
        db = new SQLiteAccessHelper(context);

        // current year diary list 초기화
        Calendar start = Calendar.getInstance();
        start.set(Calendar.MONTH, 1);
        start.set(Calendar.DATE, 1);

        Calendar end = Calendar.getInstance();
        end.set(Calendar.MONTH, 12);
        end.set(Calendar.DATE, 31);

        currentYearDiaryList = db.selectDiaryList(start, end);

        // feel list 초기화
        feelList = db.selectFeelList();

        setCalendarDecorator();
    }

    private void setCalendarDecorator() {
        for(Feel feel : feelList){
            List<CalendarDay> calendarDayList = new ArrayList<>();
            for(Diary diary : currentYearDiaryList){
                calendarDayList.add(CalendarDay.from(diary.getCalendar()));
            }
            FeelDayDecorator decorator = new FeelDayDecorator(feel, calendarDayList, this);
            feelDayDecoratorList.add(decorator);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        init(this.getApplicationContext());

        // Toolbar 초기화
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        // Test 버튼
        // TODO: 슬라이드 하거나 새로운 액션으로 액티비티 변경이 필요합니다.
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                /**
                 * Temporary codes for testing statistics activity
                 */
                Intent intent = new Intent(getApplicationContext(),StatisticsActivity.class);
                startActivity(intent);

                // TODO: 이곳에 새로 diary 생성하는게 좋을 것 같음...
            }
        });

        // Calendar 현재 시간으로 초기화
        Calendar currentCalendar = Calendar.getInstance();
        // MaterialCalendarView 생성
        MaterialCalendarView calendarView = findViewById(R.id.calendar_main);
        calendarView.setTileHeight(100); // tile 크기 조정에 따라 calendar height 가 결정됩니다.
        // 그 전에는 default로 calendar가 잡을 수 있는 가장 큰 크기를 잡습니다.
        //calendarView.setCurrentDate(currentCalendar);

        // 달력에 feel 표시해 줄 day decorator 추가
        calendarView.addDecorators(
                new SundayDecorator(),
                new SaturdayDecorator());
        calendarView.addDecorators(feelDayDecoratorList);

        // 현재 날짜 Diary Preview 생성
        showPreviewDiary(currentCalendar);

        // 날짜 선택 시 이벤트
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                // TODO: calendarView에서 이미 선택된 date와 이 listener 이벤트에서 가져온 date를 비교해서
                // TODO: 같은 경우 두번 클릭으로 보고 diary 생성하는 페이지로 넘어가도 됩니다.
                // long beforeDate = view.getDate();

                // 날짜 선택할 때 마다 해당 날짜의 diary를 가져옵니다.
                showPreviewDiary(date.getCalendar());
            }
        });
    }

    /**
     * 메모리상에 들고 있는 diary list 중 param calendar에 해당하는 diary를 가져오는 메소드입니다.
     *
     * @param calendar
     * @return Diary
     * */
    Diary getDiary(Calendar calendar){
        for(Diary diary : currentYearDiaryList){
            if(calendar.get(Calendar.YEAR)== diary.getCalendar().get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == diary.getCalendar().get(Calendar.MONTH) &&
                    calendar.get(Calendar.DATE) == diary.getCalendar().get(Calendar.DATE)){
                return diary;
            }
        }
        return null;
    }

    /**
     * 현재 calendar에 맞는 diary를 가져오고, 이를 viewing 하는 메소드입니다.
     *
     * @param calendar
     * */
    void showPreviewDiary(Calendar calendar){
        Diary currentDateDiary = getDiary(calendar);

        TextView previewTitleText = findViewById(R.id.text_title_preview);
        TextView previewDiaryText = findViewById(R.id.text_diary_preview);
        LinearLayout previewFeelLayout = findViewById(R.id.layout_feel_preview);

        // 1) title 설정
        previewTitleText.setText(DateConvertUtil.dateToStringForView(calendar));
        // 2) diary preview 설정
        if(currentDateDiary != null){
            previewDiaryText.setText(currentDateDiary.getDescription());
            // 3) feel preview 설정
            ImageView feelImage = new ImageView(this);
            feelImage.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int feelResourceId = feelMappingComponent.getResourceId(feelList, currentDateDiary.getFeelName());
            feelImage.setImageResource(feelResourceId);

            feelImage.setImageResource(R.drawable.btn_round_rectangle); // TODO : feel image로 바꾸어야 함
            previewFeelLayout.addView(feelImage);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
