package android.scholboy.com.twittersearches;

import android.app.ListActivity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity {

    private static final String SEARCHES = "searches";
    private EditText queryEditText;
    private EditText tagEditText;
    private SharedPreferences savedSearches; //запросы пользователя
    private ArrayList<String>tags; //список тегов для запросов
    private ArrayAdapter<String>adapter; //связывает теги с ListView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //---------------------полученние ссылок на EditText
        queryEditText = (EditText)findViewById(R.id.queryEditText);
        tagEditText = (EditText)findViewById(R.id.tagEditText);

        //получени обьекта sharedPreferences с сохраненыи запросм
        savedSearches = getSharedPreferences(SEARCHES,MODE_PRIVATE);

        //создание тэгов в ArrayList и их сортировка
        tags = new ArrayList<String>(savedSearches.getAll().keySet());
        Collections.sort(tags,String.CASE_INSENSITIVE_ORDER);

        //создание обьекта ArrayAdapter и привязка тегов к ListView
        adapter = new ArrayAdapter<String>(this, R.layout.list_item,tags);
        setListAdapter(adapter);

        //регистрация слушателя для сохранение запроса
        ImageButton saveButton = (ImageButton)findViewById(R.id.saveButton);
        saveButton.setOnClickListener(saveButtonListener);

        //регистрация слушателя для поиска в твиттер
        getListView().setOnItemClickListener(itemClickListener);

        //слушатель, позволяющий удалить или изменить запрос
        getListView().setOnItemLongClickListener(itemLongClickListener);

    }

    //saveButtonListener сохраняет пару "тег-значение" в SharedPreferences

    public OnClickListener saveButtonListener = new OnClickListener(){

        @Override
        public void onClick(View v)
        {
            //Создаем тег, если в queryEditText and tagEditText есть данные
            if (queryEditText.getText().length() > 0 &&
                tagEditText.getText().length()   > 0)
            {


                addTaggedSearch(queryEditText.getText().toString(),
                        tagEditText.getText().toString());

                queryEditText.setText("");//очистка queryEditText
                tagEditText.setText("");//очистка tagEditText

                ((InputMethodManager)getSystemService(
                   Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                        tagEditText.getWindowToken(),0);
            }
            else // вывод сообщеня с предложеним ввести запрос и тег
            {
                //создаем объьект AlertDialog
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(MainActivity.this);

                //назначаеи заголовок и сообщение диологового окна
                builder.setMessage(R.string.missigMessage);

                //кнопка ок просто закрывает окно
                builder.setPositiveButton(R.string.OK,null);

                //создание объекта AlertDialog на базе AlertDialog.Builder
                AlertDialog errorDialog = builder.create();
                errorDialog.show();

            }

        }
    };

    //добавление нового запроса в файд и обновление всех кнопок
    private void addTaggedSearch(String query, String tag){
        //получение объекта sharedPreference. Editor для сохранени новой пары
        SharedPreferences.Editor preferenceEditor = savedSearches.edit();
        if(savedSearches.contains(query))
        {
            Toast.makeText(this,R.string.Error_Query,Toast.LENGTH_SHORT).show();
        }else{

            preferenceEditor.putString(tag,query);//сохранение текущего запроса
            preferenceEditor.apply();//сохранение обновленных данных
        }

        //если тег только что создан - добавить и отсортеровать теги
        if(!tags.contains(tag))
        {
            tags.add(tag);
            Collections.sort(tags,String.CASE_INSENSITIVE_ORDER);
            adapter.notifyDataSetChanged();//повторное связывание с ListView
        }
    }

    //itemClickListener запускает браузер для вывода результата
    OnItemClickListener itemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //получение строки зоапроса и создание URL - адресса для запроса
            String tag = ((TextView) view).getText().toString();

            String urlString = getString(R.string.searchURL) +
                    Uri.encode(savedSearches.getString(tag,""),"utf-8");

            //создание интента для запуска браузера
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
               Uri.parse(urlString));

            startActivity(webIntent);
        }
    };

    //itemLongClickListener отображает диалоговое окно для удаление или изменение сохраненого запроса

    OnItemLongClickListener itemLongClickListener =
            new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
                {
                    //получение тега на котором было сделано длинное нажатие
                    final String tag = ((TextView)view).getText().toString();

                    //создание AlertVIew
                    AlertDialog.Builder builder =
                            new AlertDialog.Builder(MainActivity.this);

                    //заголовок
                    builder.setTitle(
                            getString(R.string.shateEditDeleteTitle,tag));

                    //список вариантов
                    builder.setItems(R.array.dialg_items,
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //регистрирует на действие ползователя пересылкой
                                    //изменением или удалением сохраненого запроса
                                    switch (which)
                                    {
                                        case 0://пересылка
                                            shareSearch(tag);
                                            break;
                                        case 1://изменение
                                            //заполнени EditText тегом м запросом
                                            tagEditText.setText(tag);

                                             queryEditText.setText(
                                                     savedSearches.getString(tag, ""));
                                            break;
                                        case 2://удаление
                                            deleteSearch(tag);
                                            break;

                                    }
                                }
                            }
                    );

                    builder.setNegativeButton(getString(R.string.cancel),
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();//закрытие
                                }
                            }
                    );
                    builder.create().show();
                    return true;
                }
            };
    //выбор приложения для пересылки url-адреса сохраненого запроса
    private void shareSearch(String tag)
    {
        //создание url-адреса представляющий запрос
        String urlString = getString(R.string.searchURL)+
            Uri.encode(savedSearches.getString(tag, ""),"utf-8");

        //создание обьекта intent для пересыдки urlString
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT,
            getString(R.string.shareSubject));
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.shareMessage, urlString));
        shareIntent.setType("text/plain");

        //вывод списка приложений
        startActivity(Intent.createChooser(shareIntent,getString(R.string.shareSearch)));
    }

    //удаление запроса после подтверждения операции пользователем
    private void deleteSearch(final String tag)
    {
        //создание AlertView диалог
        AlertDialog.Builder confirmButton = new AlertDialog.Builder(this);

        //назначения сообщения AlertDialog
        confirmButton.setMessage(
                getString(R.string.confirmMessage, tag));

        //назначение негативной кнопки AlertDialog
        confirmButton.setNegativeButton
        (getString(R.string.cancel),
                new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.cancel();
                    }
                }
        );

        //назначение позитивной кнопки
        confirmButton.setPositiveButton(getString(R.string.delete),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        tags.remove(tag);//удаление тега из коллекции

                        //получение SharedPreferences.Editor для удаления запроса
                        SharedPreferences.Editor preferenceEditor =
                                savedSearches.edit();

                        preferenceEditor.remove(tag);//удаление запроса
                        preferenceEditor.apply();//сохранение изменений

                        //повторное связывание
                        adapter.notifyDataSetChanged();
                    }
                }
        );

        confirmButton.create().show();//вывод
    }
}
