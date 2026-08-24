import { useMutation } from "@tanstack/react-query";
import { deleteDocument } from "../api";
import { useDocumentStore } from "../stores/useDocumentStore";

export function useDeleteDocument() {
  const removeDocument = useDocumentStore((state) => state.removeDocument);

  return useMutation({
    mutationFn: (documentId: string) => deleteDocument(documentId),
    onMutate: (documentId: string) => {
      // Optimistically remove from store
      removeDocument(documentId);
    },
    onError: (_err, _documentId) => {
      // If error occurs, could trigger a refresh or toast
    },
  });
}
