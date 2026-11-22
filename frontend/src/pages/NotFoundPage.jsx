import React from 'react';
import { Link } from 'react-router-dom';
export default function NotFoundPage() { return <main className="min-h-screen bg-surface text-on-surface flex flex-col items-center justify-center gap-5 px-5 text-center"><span className="text-primary font-bold text-7xl">404</span><h1 className="text-2xl font-bold">Không tìm thấy trang</h1><Link className="bg-primary-container text-white px-5 py-3 rounded-lg" to="/">Về trang chủ</Link></main>; }
