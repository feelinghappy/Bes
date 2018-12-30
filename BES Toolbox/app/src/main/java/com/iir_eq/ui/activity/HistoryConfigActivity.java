package com.iir_eq.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.iir_eq.R;
import com.iir_eq.db.DataHelper;

import java.util.List;

import butterknife.BindView;

/**
 * Created by zhaowanxing on 2017/4/23.
 */

public class HistoryConfigActivity extends BaseActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    @BindView(R.id.configs)
    ListView mConfigs;

    public static final String EXTRA_CONFIG_NAME = "extra_config_name";

    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_history_config);
        initView();
    }

    private void initView() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mAdapter = new ArrayAdapter<String>(this, R.layout.config_item, R.id.name);
        mConfigs.setAdapter(mAdapter);
        mConfigs.setOnItemClickListener(this);
        mConfigs.setOnItemLongClickListener(this);
        new LoadTask().execute();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_NAME, mAdapter.getItem(position));
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final String name = mAdapter.getItem(position);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(String.format(getString(R.string.delete_config_tips), name))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (DataHelper.getDataHelper(HistoryConfigActivity.this).delete(name)) {
                            mAdapter.remove(name);
                            mAdapter.notifyDataSetChanged();
                        } else {
                            showToast(getString(R.string.delete_failed));
                        }
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create();
        dialog.show();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history_config, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.clear:
                clear();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clear() {
        DataHelper.getDataHelper(this).clear();
        mAdapter.clear();
        mAdapter.notifyDataSetChanged();
    }

    private class LoadTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... params) {
            return DataHelper.getDataHelper(HistoryConfigActivity.this).getConfigs();
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            mAdapter.addAll(strings);
            mAdapter.notifyDataSetChanged();
        }
    }
}
