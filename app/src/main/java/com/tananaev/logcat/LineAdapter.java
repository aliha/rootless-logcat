/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tananaev.logcat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class LineAdapter extends RecyclerView.Adapter<LineAdapter.LineViewHolder> {

    private List<Line> linesAll = new LinkedList<>();
    private List<Line> linesFiltered = new LinkedList<>();

    @NonNull
    private String tag = "";
    @NonNull
    private String keyword = "";
    @NonNull
    private String lowerKeyword = "";
    @NonNull
    private String searchWord = "";
    @NonNull
    private String lowerSearchWord = "";

    public static class LineViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;

        public LineViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }

        public TextView getTextView() {
            return textView;
        }

    }

    public List<Line> getLines() {
        return linesFiltered;
    }

    @NonNull
    public String getKeyword() {
        return keyword;
    }

    @NonNull
    public String getTag() {
        return tag;
    }

    public void clear() {
        linesAll.clear();
        linesFiltered.clear();
        notifyDataSetChanged();
    }

    public void addItems(List<String> lines) {
        List<Line> linesAll = new LinkedList<>();
        for (String line : lines) {
            linesAll.add(new Line(line));
        }
        this.linesAll.addAll(linesAll);
        List<Line> linesFiltered = filter(linesAll);
        this.linesFiltered.addAll(linesFiltered);
        notifyItemRangeInserted(this.linesFiltered.size() - linesFiltered.size(), linesFiltered.size());
    }

    private List<Line> filter(List<Line> lines) {
        List<Line> linesFiltered = new LinkedList<>();
        String lowerTag = tag.toLowerCase();
        boolean hasKeyword = !TextUtils.isEmpty(lowerKeyword);
        boolean hasTag = !TextUtils.isEmpty(lowerTag);
        if (hasKeyword || hasTag) {
            for (Line line : lines) {
                if (hasTag && hasKeyword && line.getLowerTag().contains(lowerTag) && line.getLowerContent().contains(lowerKeyword)) {
                    linesFiltered.add(line);
                } else if (hasTag && !hasKeyword && line.getLowerTag().contains(lowerTag)) {
                    linesFiltered.add(line);
                } else if (!hasTag && hasKeyword && line.getLowerContent().contains(lowerKeyword)) {
                    linesFiltered.add(line);
                }
            }
        } else {
            linesFiltered.addAll(lines);
        }
        return linesFiltered;
    }

    public void filter(@NonNull String tag, @NonNull String keyword) {
        this.tag = tag;
        this.keyword = keyword;
        this.lowerKeyword = keyword.toLowerCase();

        linesFiltered = filter(linesAll);
        notifyDataSetChanged();
    }

    public void search(@NonNull String searchWord) {
        this.searchWord = searchWord;
        this.lowerSearchWord = searchWord.toLowerCase();
        notifyDataSetChanged();
    }

    public String getSearchWord() {
        return searchWord;
    }

    @Override
    public LineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.line_list_item, parent, false);
        return new LineViewHolder(view);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(LineViewHolder holder, int position) {
        Line item = linesFiltered.get(position);
        holder.itemView.setTag(item);
        holder.itemView.setOnLongClickListener(onItemLongClickListener);
        String text = item.getContent();
        if (!TextUtils.isEmpty(lowerKeyword) || !TextUtils.isEmpty(lowerSearchWord)) {
            SpannableString spannableText = new SpannableString(text);
            String lowerContent = item.getLowerContent();

            // filter keyword
            if (!TextUtils.isEmpty(lowerKeyword)) {

                int index = 0, found;
                while ((found = lowerContent.indexOf(lowerKeyword, index)) >= 0) {
                    spannableText.setSpan(new BackgroundColorSpan(Color.RED), found, found + lowerKeyword.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    index = found + lowerKeyword.length();
                }
            }

            // highlight search word
            if (!TextUtils.isEmpty(lowerSearchWord)) {
                int index = 0, found;
                while ((found = lowerContent.indexOf(lowerSearchWord, index)) >= 0) {
                    spannableText.setSpan(new BackgroundColorSpan(Color.YELLOW), found, found + lowerSearchWord.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    index = found + lowerSearchWord.length();
                }
            }
            holder.getTextView().setText(spannableText);
        } else {
            holder.getTextView().setText(text);
        }

        Context context = holder.getTextView().getContext();
        holder.itemView.setBackgroundColor(context.getResources().getColor(position % 2 == 0 ? R.color.row_bg_color_even : R.color.row_bg_color_odd));
        switch (item.getLevel()) {
            case 'W':
                holder.getTextView().setTextColor(context.getResources().getColor(R.color.colorWarning));
                break;
            case 'E':
            case 'A':
                holder.getTextView().setTextColor(context.getResources().getColor(R.color.colorError));
                break;
            default:
                holder.getTextView().setTextColor(context.getResources().getColor(R.color.colorNormal));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return linesFiltered.size();
    }

    private final View.OnLongClickListener onItemLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            if (v.getTag() instanceof Line) {
                final Line line = (Line) v.getTag();
                final Context context = v.getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                String[] menuItems = new String[]{
                        context.getString(R.string.copy_text),
                        context.getString(R.string.pretty_json)
                };
                builder.setItems(menuItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            setClipboardText(context, line.getContent());
                        } else {
                            showPrettyJsonDialog(context, line.getContent());
                        }
                    }
                });
                builder.show();
                return true;
            }
            return false;
        }
    };

    private void setClipboardText(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, R.string.done, Toast.LENGTH_SHORT).show();
    }

    private void showPrettyJsonDialog(final Context context, String text) {
        try {
            int index = text.indexOf("{");
            JSONObject jsonObject = new JSONObject(text.substring(index));
            final String jsonString = jsonObject.toString(2);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(jsonString);
            builder.setNegativeButton(R.string.copy_text, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setClipboardText(context, jsonString);
                }
            });
            builder.setPositiveButton(R.string.warning_close, null);
            builder.show();
        } catch (Exception ex) {
            Toast.makeText(context, R.string.not_json_string, Toast.LENGTH_SHORT).show();
        }
    }

}
