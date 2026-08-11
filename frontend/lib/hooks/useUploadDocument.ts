import { useMutation } from "@tanstack/react-query";
import { uploadDocument } from "../api";
import { useDocumentStore } from "../stores/useDocumentStore";

export function useUploadDocument() {
  const addDocument = useDocumentStore((state) => state.addDocument);

  return useMutation({
    mutationFn: (data: { file: File; sourceType?: string }) =>
      uploadDocument(data.file, data.sourceType),
    onSuccess: (data) => {
      addDocument(data);
    },
  });
}
