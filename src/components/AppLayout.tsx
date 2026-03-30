import { Outlet } from "react-router-dom";
import { AppSidebar } from "./AppSidebar";
import { SidebarInset, SidebarProvider, SidebarTrigger } from "./ui/sidebar";

export function AppLayout() {
  return (
    <SidebarProvider style={{ backgroundColor: '#020617' }}>
      <AppSidebar />
      <SidebarInset style={{ backgroundColor: '#020617', border: 'none' }}>
        <header style={{ display: 'flex', height: '4rem', alignItems: 'center', paddingLeft: '1.5rem', paddingRight: '1.5rem', borderBottom: '1px solid #0f172a' }}>
          <SidebarTrigger style={{ color: 'white' }} />
        </header>
        <main style={{ flex: 1, padding: '1.5rem', overflowY: 'auto' }}>
          <Outlet />
        </main>
      </SidebarInset>
    </SidebarProvider>
  );
}
