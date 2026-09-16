import NextAuth from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";

const handler = NextAuth({
  providers: [
    CredentialsProvider({
      name: "Credentials",
      credentials: {
        email: { label: "Email", type: "email", placeholder: "you@example.com" },
        password: { label: "Password", type: "password" }
      },
      async authorize(credentials) {
        // In a real app, we would POST to our Spring Boot /api/v1/auth/login endpoint
        // For the LexPilot demo, we'll accept any email and mint a mock user
        if (credentials?.email && credentials?.password) {
          return {
            id: "1",
            email: credentials.email,
            name: credentials.email.split('@')[0],
            role: "USER",
            tenantId: "00000000-0000-0000-0000-000000000000"
          };
        }
        return null;
      }
    })
  ],
  callbacks: {
    async jwt({ token, user }) {
      if (user) {
        token.role = (user as any).role;
        token.tenantId = (user as any).tenantId;
      }
      return token;
    },
    async session({ session, token }) {
      if (session.user) {
        (session.user as any).role = token.role;
        (session.user as any).tenantId = token.tenantId;
      }
      return session;
    }
  },
  session: {
    strategy: "jwt"
  },
  pages: {
    signIn: "/login"
  }
});

export { handler as GET, handler as POST };
