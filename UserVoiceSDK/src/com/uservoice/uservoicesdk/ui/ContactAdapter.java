package com.uservoice.uservoicesdk.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.uservoice.uservoicesdk.R;
import com.uservoice.uservoicesdk.Session;
import com.uservoice.uservoicesdk.babayaga.Babayaga;
import com.uservoice.uservoicesdk.babayaga.Babayaga.Event;
import com.uservoice.uservoicesdk.model.CustomField;
import com.uservoice.uservoicesdk.model.Ticket;

public class ContactAdapter extends InstantAnswersAdapter {

	private int CUSTOM_TEXT_FIELD = 8;
	private int CUSTOM_PREDEFINED_FIELD = 9;

	private Map<String, String> customFieldValues;

	public ContactAdapter(Activity context) {
		super(context);
		customFieldValues = new HashMap<String, String>(Session.getInstance().getConfig().getCustomFields());
	}

	@Override
	public int getViewTypeCount() {
		return super.getViewTypeCount() + 2;
	}

	@Override
	protected List<Integer> getDetailRows() {
		List<Integer> rows = new ArrayList<Integer>();
		rows.addAll(Arrays.asList(EMAIL_FIELD, NAME_FIELD, SPACE));
		for (CustomField customField : Session.getInstance().getClientConfig().getCustomFields()) {
			if (customField.isPredefined())
				rows.add(CUSTOM_PREDEFINED_FIELD);
			else
				rows.add(CUSTOM_TEXT_FIELD);
		}
		return rows;
	}

	@Override
	public Object getItem(int position) {
		int type = getItemViewType(position);
		if (type == CUSTOM_PREDEFINED_FIELD || type == CUSTOM_TEXT_FIELD) {
			int offset = (instantAnswers.isEmpty() ? 5 : 6) + Math.min(3, instantAnswers.size());
			return Session.getInstance().getClientConfig().getCustomFields().get(position - offset);
		} else {
			return super.getItem(position);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		int type = getItemViewType(position);
		if (view == null) {
			if (type == CUSTOM_TEXT_FIELD) {
				view = inflater.inflate(R.layout.text_field_item, null);
			} else if (type == CUSTOM_PREDEFINED_FIELD) {
				view = inflater.inflate(R.layout.select_field_item, null);
			} else {
				return super.getView(position, convertView, parent);
			}
		}

		if (type == CUSTOM_TEXT_FIELD) {
			TextView title = (TextView) view.findViewById(R.id.header_text);
			final EditText field = (EditText) view.findViewById(R.id.text_field);
			final CustomField customField = (CustomField) getItem(position);
			String value = customFieldValues.get(customField.getName());
			title.setText(customField.getName());
			field.setHint(R.string.value);
			field.setInputType(EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
			field.setText(value);
			field.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						customFieldValues.put(customField.getName(), field.getText().toString());
					}
				}
			});
		} else if (type == CUSTOM_PREDEFINED_FIELD) {
			final CustomField customField = (CustomField) getItem(position);
			String value = customFieldValues.get(customField.getName());
			TextView title = (TextView) view.findViewById(R.id.header_text);
			title.setText(customField.getName());
			Spinner field = (Spinner) view.findViewById(R.id.select_field);
			field.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					customFieldValues.put(customField.getName(), customField.getPredefinedValues().get(position));
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
			field.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, customField.getPredefinedValues()));
			if (value != null && customField.getPredefinedValues().contains(value))
				field.setSelection(customField.getPredefinedValues().indexOf(value));
		} else {
			return super.getView(position, convertView, parent);
		}
		return view;
	}

	@Override
	protected void doSubmit() {
		Ticket.createTicket(textField.getText().toString(), emailField.getText().toString(), nameField.getText().toString(), customFieldValues, new DefaultCallback<Ticket>(context) {
			@Override
			public void onModel(Ticket model) {
				Babayaga.track(Event.SUBMIT_TICKET);
				Toast.makeText(context, R.string.msg_ticket_created, Toast.LENGTH_SHORT).show();
				context.finish();
			}
		});
	}

	@Override
	protected String getSubmitString() {
		return context.getString(R.string.send_message);
	}

}