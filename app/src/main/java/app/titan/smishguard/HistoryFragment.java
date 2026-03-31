package app.titan.smishguard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private ListView listView;
    private SmishRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = view.findViewById(R.id.historyListView);
        repo = new SmishRepository(requireContext());

        loadHistoryData();
    }

    private void loadHistoryData() {
        if (repo == null || listView == null) return;

        // Fetch from shared prefs
        List<String> history = repo.getRecentVerdicts();

        if (history != null && !history.isEmpty()) {
            ArrayList<String> dataList = new ArrayList<>(history);
            SMSAdapter adapter = new SMSAdapter(requireContext(), dataList);
            listView.setAdapter(adapter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // This ensures that if a scam is caught while the app is open,
        // switching to the History tab immediately shows it.
        loadHistoryData();
    }
}