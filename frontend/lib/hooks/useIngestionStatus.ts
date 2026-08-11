import { useQuery } from "@tanstack/react-query";
import { getIngestionStatus } from "../api";
import { useDocumentStore } from "../stores/useDocumentStore";

export function useIngestionStatus(documentId: string) {
  const updateStatus = useDocumentStore((state) => state.updateStatus);

  return useQuery({
    queryKey: ["ingestionStatus", documentId],
    queryFn: async () => {
      const data = await getIngestionStatus(documentId);
      updateStatus(data);
      return data;
    },
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === "INDEXED" || status === "FAILED") {
        return false; // Stop polling
      }
      return 3000; // Poll every 3 seconds
    },
    enabled: !!documentId,
  });
}
